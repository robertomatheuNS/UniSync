import test from 'node:test'
import assert from 'node:assert/strict'
import { buildMonthCells } from '../../src/utils/calendar.js'
import { obterSessao } from '../../src/utils/sessao.js'
import {
  dataOportunidade,
  dataParaFormulario,
} from '../../src/utils/dataOportunidade.js'
test('calendário inclui ano bissexto e alinhamento', () => {
  const dias = buildMonthCells(new Date(2028, 1, 1))
  assert.equal(dias.filter(Boolean).length, 29)
  assert.equal(dias[2], 1)
  assert.equal(buildMonthCells(new Date(2027, 1, 1)).filter(Boolean).length, 28)
})
test('sessão suporta Unicode e rejeita token expirado', () => {
  const token = (claims) =>
    'e30.' +
    Buffer.from(JSON.stringify(claims)).toString('base64url') +
    '.teste'
  global.window = {
    UNISYNC_SHELL: {
      accessToken: token({
        exp: Date.now() / 1000 + 3600,
        nameid: 'id',
        unique_name: 'João',
        role: 'Aluno',
      }),
    },
  }
  assert.equal(obterSessao().nome, 'João')
  window.UNISYNC_SHELL.accessToken = token({ exp: 1 })
  assert.equal(obterSessao(), null)
  window.UNISYNC_SHELL.accessToken = 'invalido'
  assert.equal(obterSessao(), null)
})
test('data sem fuso é interpretada como UTC', () => {
  assert.equal(
    dataOportunidade('2028-01-01T12:00:00').toISOString(),
    '2028-01-01T12:00:00.000Z',
  )
  assert.equal(dataParaFormulario('2028-01-01T12:00:00Z').length, 16)
})
