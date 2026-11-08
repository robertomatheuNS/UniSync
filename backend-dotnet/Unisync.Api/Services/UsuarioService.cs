
using Microsoft.AspNetCore.Identity;
using Microsoft.IdentityModel.Tokens;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Unisync.Api.DTOs;
using Unisync.Api.Entities;
using Unisync.Api.Repositories;

namespace Unisync.Api.Services;

public class UsuarioService(
    UsuarioRepositoryInterface usuarioRepository,
    IPasswordHasher<Usuario> passwordHasher,
    IConfiguration configuration) : UsuarioServiceInterface
{
    public Task<CriarUsuarioResultado> CriarAsync(
        CriarUsuarioRequest request,
        CancellationToken cancellationToken) =>
        CriarUsuarioAsync(
            request,
            static (nome, email) => new Aluno(nome, email),
            async (aluno, token) => await usuarioRepository.AdicionarAsync(aluno, token),
            cancellationToken);

    public Task<CriarUsuarioResultado> CriarProfessorAsync(
        CriarProfessorRequest request,
        CancellationToken cancellationToken) =>
        CriarUsuarioAsync(
            request,
            static (nome, email) => new Professor(nome, email),
            async (professor, token) => await usuarioRepository.AdicionarProfessorComCodigoAsync(
                professor, request.CodigoProfessor, token) ? professor : null,
            cancellationToken);

    private async Task<CriarUsuarioResultado> CriarUsuarioAsync<TUsuario>(
        CriarUsuarioRequest request,
        Func<string, string, TUsuario> criarUsuario,
        Func<TUsuario, CancellationToken, Task<Usuario?>> persistirUsuario,
        CancellationToken cancellationToken) where TUsuario : Usuario
    {
        var nome = request.Nome.Trim();
        var email = request.Email.Trim().ToLowerInvariant();

        if (await usuarioRepository.EmailExisteAsync(email, cancellationToken))
        {
            return new CriarUsuarioResultado(CriarUsuarioStatus.EmailJaCadastrado, null);
        }

        var usuario = criarUsuario(nome, email);
        usuario.DefinirSenhaHash(passwordHasher.HashPassword(usuario, request.Senha));

        try
        {
            var usuarioPersistido = await persistirUsuario(usuario, cancellationToken);
            return usuarioPersistido is null
                ? new CriarUsuarioResultado(CriarUsuarioStatus.CodigoProfessorIndisponivel, null)
                : new CriarUsuarioResultado(CriarUsuarioStatus.Criado, ParaResponse(usuarioPersistido));
        }
        catch (EmailUsuarioJaCadastradoException)
        {
            return new CriarUsuarioResultado(CriarUsuarioStatus.EmailJaCadastrado, null);
        }
    }

    public async Task<IReadOnlyList<UsuarioResponse>> ListarAsync(CancellationToken cancellationToken)
    {
        var usuarios = await usuarioRepository.ListarAsync(cancellationToken);
        return usuarios.Select(ParaResponse).ToList();
    }

    public async Task<UsuarioResponse?> ObterPorIdAsync(Guid id, CancellationToken cancellationToken)
    {
        var usuario = await usuarioRepository.ObterPorIdAsync(id, cancellationToken);
        return usuario is null ? null : ParaResponse(usuario);
    }

    public async Task<string?> LoginAsync(
        LoginRequest request,
        CancellationToken cancellationToken)
    {
        var email = request.Email.Trim().ToLowerInvariant();

        var usuario = await usuarioRepository.ObterPorEmailAsync(
            email, cancellationToken);

        if (usuario is null)
        {
            return null;
        }

        var resultadoSenha = passwordHasher.VerifyHashedPassword(
            usuario, usuario.SenhaHash, request.Senha);

        if (resultadoSenha == PasswordVerificationResult.Failed)
        {
            return null;
        }

        var chave = Convert.FromBase64String(
            configuration["Jwt:Key"]
            ?? throw new InvalidOperationException("Chave JWT não configurada."));

        var issuer = configuration["Jwt:Issuer"]
            ?? throw new InvalidOperationException("Issuer JWT não configurado.");

        var audience = configuration["Jwt:Audience"]
            ?? throw new InvalidOperationException("Audience JWT não configurado.");

        var claims = new[]
        {
            new Claim(ClaimTypes.NameIdentifier, usuario.Id.ToString()),
            new Claim(ClaimTypes.Name, usuario.Nome),
            new Claim(ClaimTypes.Email, usuario.Email)
        };

        var credentials = new SigningCredentials(
            new SymmetricSecurityKey(chave),
            SecurityAlgorithms.HmacSha256);

        var token = new JwtSecurityToken(
            issuer: issuer,
            audience: audience,
            claims: claims,
            expires: DateTime.UtcNow.AddHours(1),
            signingCredentials: credentials);

        return new JwtSecurityTokenHandler().WriteToken(token);
    }

    private static UsuarioResponse ParaResponse(Usuario usuario) =>
        new(usuario.Id, usuario.Nome, usuario.Email);
}
