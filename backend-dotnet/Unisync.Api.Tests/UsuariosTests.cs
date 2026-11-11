using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using Npgsql;
using Unisync.Api.Comandos;
using Unisync.Api.DTOs;
using Xunit;

namespace Unisync.Api.Tests;

public sealed class UsuariosTests(ApiFixture api) : IClassFixture<ApiFixture>
{
    private const string Usuarios = "/api/usuarios";
    private const string Professores = Usuarios + "/professores";
    private const string Login = Usuarios + "/login";
    private const string Senha = "SenhaTeste123";

    [Theory]
    [InlineData(ApiFixture.OrigemFrontend, true)]
    [InlineData("http://localhost:5174", false)]
    public async Task PreflightDeLoginSoAutorizaOrigemConfigurada(string origem, bool permitida)
    {
        using var cliente = api.CriarCliente();
        using var preflight = new HttpRequestMessage(HttpMethod.Options, Login);
        preflight.Headers.Add("Origin", origem);
        preflight.Headers.Add("Access-Control-Request-Method", "POST");
        preflight.Headers.Add("Access-Control-Request-Headers", "content-type");

        using var resposta = await cliente.SendAsync(preflight);
        Assert.Equal(HttpStatusCode.NoContent, resposta.StatusCode);
        if (permitida)
        {
            Assert.Equal(origem, Assert.Single(resposta.Headers.GetValues("Access-Control-Allow-Origin")));
            Assert.Contains("POST", resposta.Headers.GetValues("Access-Control-Allow-Methods"));
        }
        else
        {
            Assert.False(resposta.Headers.Contains("Access-Control-Allow-Origin"));
        }
    }

    [Fact]
    public async Task OrigemAutorizadaRecebeCorsMesmoQuandoFaltaToken()
    {
        using var cliente = api.CriarCliente();
        using var request = new HttpRequestMessage(HttpMethod.Get, Usuarios);
        request.Headers.Add("Origin", ApiFixture.OrigemFrontend);

        using var resposta = await cliente.SendAsync(request);
        Assert.Equal(HttpStatusCode.Unauthorized, resposta.StatusCode);
        Assert.Equal(ApiFixture.OrigemFrontend,
            Assert.Single(resposta.Headers.GetValues("Access-Control-Allow-Origin")));
    }

    [Fact]
    public async Task AlunoPodeCadastrarEEntrarMasNaoPodeListarUsuarios()
    {
        using var cliente = api.CriarCliente();
        var cadastro = new CriarUsuarioRequest
        {
            Nome = "Aluno Teste",
            Email = "aluno@example.invalid",
            Senha = Senha
        };

        using var anonimo = await cliente.GetAsync(Usuarios);
        Assert.Equal(HttpStatusCode.Unauthorized, anonimo.StatusCode);

        using var criado = await cliente.PostAsJsonAsync(Usuarios, cadastro);
        Assert.Equal(HttpStatusCode.Created, criado.StatusCode);

        using var duplicado = await cliente.PostAsJsonAsync(Usuarios, cadastro);
        Assert.Equal(HttpStatusCode.Conflict, duplicado.StatusCode);

        using var senhaErrada = await cliente.PostAsJsonAsync(Login,
            new LoginRequest { Email = cadastro.Email, Senha = "SenhaIncorreta" });
        Assert.Equal(HttpStatusCode.Unauthorized, senhaErrada.StatusCode);

        cliente.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer",
            await EntrarAsync(cliente, cadastro.Email));
        using var proibido = await cliente.GetAsync(Usuarios);
        Assert.Equal(HttpStatusCode.Forbidden, proibido.StatusCode);
    }

    [Fact]
    public async Task ProfessorPrecisaDeConviteValidoEDeUsoUnico()
    {
        using var cliente = api.CriarCliente();
        var cadastro = new CriarProfessorRequest
        {
            Nome = "Professor Teste",
            Email = "professor@example.invalid",
            Senha = Senha,
            CodigoProfessor = Guid.NewGuid().ToString()
        };

        using var invalido = await cliente.PostAsJsonAsync(Professores, cadastro);
        Assert.Equal(HttpStatusCode.BadRequest, invalido.StatusCode);

        Assert.Equal(0, await GerarCodigosProfessor.ExecutarAsync(
            [GerarCodigosProfessor.Nome, "1"], api.Conexao));

        await using var banco = NpgsqlDataSource.Create(api.Conexao);
        await using var consulta = banco.CreateCommand("""
            SELECT codigo FROM public."Codigo_professor" WHERE NOT ultilizado
            """);
        cadastro.CodigoProfessor = Assert.IsType<Guid>(await consulta.ExecuteScalarAsync()).ToString();

        using var criado = await cliente.PostAsJsonAsync(Professores, cadastro);
        Assert.Equal(HttpStatusCode.Created, criado.StatusCode);
        var professor = Assert.IsType<UsuarioResponse>(
            await criado.Content.ReadFromJsonAsync<UsuarioResponse>());

        cadastro.Email = "outro@example.invalid";
        using var reutilizado = await cliente.PostAsJsonAsync(Professores, cadastro);
        Assert.Equal(HttpStatusCode.BadRequest, reutilizado.StatusCode);

        cliente.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer",
            await EntrarAsync(cliente, professor.Email));
        using var listagem = await cliente.GetAsync(Usuarios);
        Assert.Equal(HttpStatusCode.OK, listagem.StatusCode);
        var usuarios = Assert.IsType<UsuarioResponse[]>(
            await listagem.Content.ReadFromJsonAsync<UsuarioResponse[]>());
        Assert.Contains(usuarios, usuario => usuario.Id == professor.Id);
        Assert.DoesNotContain(usuarios, usuario => usuario.Email == cadastro.Email);
    }

    private static async Task<string> EntrarAsync(HttpClient cliente, string email)
    {
        using var resposta = await cliente.PostAsJsonAsync(Login,
            new LoginRequest { Email = email, Senha = Senha });
        Assert.Equal(HttpStatusCode.OK, resposta.StatusCode);
        var token = Assert.IsType<TokenResponse>(await resposta.Content.ReadFromJsonAsync<TokenResponse>());
        Assert.False(string.IsNullOrWhiteSpace(token.Token));
        return token.Token;
    }

    private sealed record TokenResponse(string Token);
}
