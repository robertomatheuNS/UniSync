using Unisync.Api.DTOs;

namespace Unisync.Api.Services;

public interface UsuarioServiceInterface
{
    Task<CriarUsuarioResultado> CriarAsync(CriarUsuarioRequest request, CancellationToken cancellationToken);
    Task<IReadOnlyList<UsuarioResponse>> ListarAsync(CancellationToken cancellationToken);
    Task<UsuarioResponse?> ObterPorIdAsync(Guid id, CancellationToken cancellationToken);
    Task<string?> LoginAsync(LoginRequest request, CancellationToken cancellationToken);
}

public enum CriarUsuarioStatus
{
    Criado,
    EmailJaCadastrado
}

public record CriarUsuarioResultado(CriarUsuarioStatus Status, UsuarioResponse? Usuario);
