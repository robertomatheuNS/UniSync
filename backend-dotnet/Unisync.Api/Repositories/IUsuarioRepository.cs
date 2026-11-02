using Unisync.Api.Entities;

namespace Unisync.Api.Repositories;

public interface IUsuarioRepository
{
    Task<bool> EmailExisteAsync(string email, CancellationToken cancellationToken);
    Task<Usuario> AdicionarAsync(Usuario usuario, CancellationToken cancellationToken);
    Task<Usuario?> ObterPorIdAsync(Guid id, CancellationToken cancellationToken);
    Task<IReadOnlyList<Usuario>> ListarAsync(CancellationToken cancellationToken);
}