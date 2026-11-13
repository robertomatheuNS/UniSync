import { test, expect } from '@playwright/test'

async function preparar(page, papel = 'Aluno') {
  const token =
    'e30.' +
    Buffer.from(
      JSON.stringify({
        exp: Date.now() / 1000 + 3600,
        nameid: 'usuario-teste',
        unique_name: 'Pessoa teste',
        role: papel,
      }),
    ).toString('base64url') +
    '.teste'
  await page.addInitScript((token) => {
    window.UNISYNC_SHELL = { accessToken: token }
  }, token)
  let lembretes = [],
    tarefas = []
  const oportunidade = {
    id: 'o1',
    professor_id: 'outro',
    professor: 'Professor',
    titulo: 'Palestra de programação',
    materia: 'Programação',
    tipo: 'EVENTO',
    descricao: 'Encontro no laboratório 2.',
    data_evento: '2028-03-20T15:00:00Z',
  }
  await page.route('**/oportunidades/**', (r) =>
    r.fulfill({
      json: r.request().url().includes('/listar')
        ? [oportunidade]
        : oportunidade,
    }),
  )
  await page.route(/\/(tarefas|lembretes)\//, async (route) => {
    const request = route.request(),
      url = new URL(request.url()),
      id = url.pathname.split('/').pop(),
      lembrete = url.pathname.startsWith('/lembretes')
    expect(request.headers().authorization).toBe('Bearer ' + token)
    let itens = lembrete ? lembretes : tarefas,
      resultado
    if (request.method() === 'GET') resultado = itens
    else if (request.method() === 'POST') {
      resultado = {
        id: crypto.randomUUID(),
        concluido: false,
        status: 'A_FAZER',
        ...request.postDataJSON(),
      }
      itens.push(resultado)
    } else if (request.method() === 'PATCH') {
      resultado = Object.assign(
        itens.find((item) => item.id === id),
        request.postDataJSON(),
      )
    } else if (request.method() === 'DELETE') {
      if (lembrete) lembretes = itens.filter((item) => item.id !== id)
      else tarefas = itens.filter((item) => item.id !== id)
      await route.fulfill({ status: 204 })
      return
    }
    await route.fulfill({ json: resultado })
  })
}

test('dashboard, calendário e lembretes persistem ao recarregar', async ({
  page,
}) => {
  await preparar(page)
  await page.goto('/app')
  await page
    .getByRole('button', { name: 'Ver detalhes de Palestra de programação' })
    .click()
  await expect(page.getByText('Encontro no laboratório 2.')).toBeVisible()
  await expect(
    page.getByRole('button', { name: 'Editar oportunidade' }),
  ).toHaveCount(0)
  await page.getByRole('button', { name: 'Fechar janela' }).click()
  const calendario = page.getByRole('region', { name: 'Calendário' })
  const mes = await calendario.locator('h2').textContent()
  await calendario.getByRole('button', { name: 'Próximo mês' }).click()
  await expect(calendario.locator('h2')).not.toHaveText(mes)
  await calendario.getByRole('button', { name: 'Hoje', exact: true }).click()
  await expect(calendario.locator('h2')).toHaveText(mes)
  const lembretes = page.getByRole('region', { name: 'Meus lembretes' })
  await lembretes.getByLabel('Novo lembrete').fill('Ler capítulo 3')
  await lembretes
    .getByRole('button', { name: 'Adicionar', exact: true })
    .click()
  await page.reload()
  await lembretes.getByRole('button', { name: 'Editar Ler capítulo 3' }).click()
  await lembretes.getByLabel('Editar lembrete').fill('Ler capítulo 4')
  await lembretes.getByRole('button', { name: 'Salvar', exact: true }).click()
  await lembretes
    .getByRole('checkbox', { name: 'Concluir Ler capítulo 4' })
    .check()
  await page.reload()
  await expect(
    lembretes.getByRole('checkbox', { name: 'Concluir Ler capítulo 4' }),
  ).toBeChecked()
  await lembretes
    .getByRole('button', { name: 'Excluir Ler capítulo 4' })
    .click()
  await expect(
    lembretes.getByRole('button', { name: 'Editar Ler capítulo 4' }),
  ).toHaveCount(0)
  await expect(page.getByRole('link', { name: 'Disciplinas' })).toHaveCount(0)
})

test('kanban cria, arrasta, edita e exclui pela lixeira', async ({ page }) => {
  await preparar(page)
  await page.goto('/app/kanban')
  await page.getByLabel('Nova tarefa', { exact: true }).fill('Estudar álgebra')
  await page
    .getByRole('main')
    .getByRole('button', { name: 'Adicionar', exact: true })
    .click()
  const tarefa = page
    .locator('.tarefa-arrastavel')
    .filter({ hasText: 'Estudar álgebra' })
  await tarefa.dragTo(page.locator('[data-coluna="doing"]'))
  await expect(
    page.locator('[data-coluna="doing"] .tarefa-arrastavel'),
  ).toHaveCount(1)
  await page.reload()
  await expect(
    page.locator('[data-coluna="doing"] .tarefa-arrastavel'),
  ).toHaveCount(1)
  await page.getByRole('button', { name: 'Editar Estudar álgebra' }).click()
  await page.getByLabel('Título', { exact: true }).fill('Revisar álgebra')
  await page.getByRole('button', { name: 'Salvar', exact: true }).click()
  await page
    .locator('.tarefa-arrastavel')
    .dragTo(page.locator('[data-coluna="trash"]'))
  await page
    .getByRole('button', { name: 'Excluir tarefa', exact: true })
    .click()
  await expect(page.locator('.tarefa-arrastavel')).toHaveCount(0)
})

test('professor tem lembretes e acesso ao Kanban é informado', async ({
  page,
}) => {
  await preparar(page, 'Professor')
  await page.goto('/app/kanban')
  await expect(
    page.getByText('O Kanban está disponível para contas de aluno.'),
  ).toBeVisible()
  await expect(
    page.getByRole('region', { name: 'Meus lembretes' }),
  ).toBeVisible()
})

test('cadastro de professor leva ao shell local', async ({ page }) => {
  await page.route('http://localhost:8081/cadastro/professor/', (r) =>
    r.fulfill({ body: 'Cadastro de professor' }),
  )
  await page.goto('/cadastro?perfil=professor')
  await expect(page).toHaveURL('http://localhost:8081/cadastro/professor/')
})

test('layout móvel e falha de salvamento preservam o lembrete', async ({
  page,
}) => {
  await preparar(page)
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/app')
  await page.getByRole('heading', { name: 'Seu painel', exact: true }).waitFor()
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBeTruthy()
  const lembretes = page.getByRole('region', { name: 'Meus lembretes' })
  await page.route('**/lembretes/criar', (r) =>
    r.fulfill({ status: 500, json: { detail: 'erro simulado' } }),
  )
  await lembretes.getByLabel('Novo lembrete').fill('Texto preservado')
  await lembretes
    .getByRole('button', { name: 'Adicionar', exact: true })
    .click()
  await expect(lembretes.getByRole('alert')).toBeVisible()
  await expect(lembretes.getByLabel('Novo lembrete')).toHaveValue(
    'Texto preservado',
  )
  await page.getByRole('switch', { name: 'Tema escuro' }).click()
  await page.screenshot({
    path: 'test-results/painel-mobile.png',
    fullPage: true,
  })
})

test('sem sessão o painel encaminha ao shell', async ({ page }) => {
  await page.route('http://localhost:8081/app/kanban', (r) =>
    r.fulfill({ body: 'Entrar pelo shell' }),
  )
  await page.goto('/app/kanban')
  await expect(page).toHaveURL('http://localhost:8081/app/kanban')
})
