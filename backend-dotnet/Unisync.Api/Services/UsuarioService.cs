using Microsoft.AspNetCore.Identity;
using Unisync.Api.DTOs;
using Unisync.Api.Entities;
using Unisync.Api.Repositories;

namespace Unisync.Api.Services;

public class UsuarioService(
    IUsuarioRepository usuarioRepository,
    IPasswordHasher<Usuario> passwordHasher) : IUsuarioService
{
    public async Task<CriarUsuarioResultado> CriarAsync(
        CriarUsuarioRequest request,
        CancellationToken cancellationToken)
    {
        var nome = request.Nome.Trim();
        var email = request.Email.Trim().ToLowerInvariant();

        if (await usuarioRepository.EmailExisteAsync(email, cancellationToken))
        {
            return new CriarUsuarioResultado(CriarUsuarioStatus.EmailJaCadastrado, null);
        }

        var usuario = new Usuario(nome, email);
        usuario.DefinirSenhaHash(passwordHasher.HashPassword(usuario, request.Senha));

        try
        {
            usuario = await usuarioRepository.AdicionarAsync(usuario, cancellationToken);
        }
        catch (EmailUsuarioJaCadastradoException)
        {
            return new CriarUsuarioResultado(CriarUsuarioStatus.EmailJaCadastrado, null);
        }

        return new CriarUsuarioResultado(CriarUsuarioStatus.Criado, ParaResponse(usuario));
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

    private static UsuarioResponse ParaResponse(Usuario usuario) =>
        new(usuario.Id, usuario.Nome, usuario.Email, usuario.DataCriacaoUtc);
}