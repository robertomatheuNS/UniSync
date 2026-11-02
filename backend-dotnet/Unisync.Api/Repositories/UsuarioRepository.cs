using Microsoft.EntityFrameworkCore;
using Npgsql;
using Unisync.Api.Data;
using Unisync.Api.Entities;

namespace Unisync.Api.Repositories;

public class UsuarioRepository(AppDbContext context) : IUsuarioRepository
{
    public Task<bool> EmailExisteAsync(string email, CancellationToken cancellationToken) =>
        context.Usuarios.AsNoTracking().AnyAsync(usuario => usuario.Email == email, cancellationToken);

    public async Task<Usuario> AdicionarAsync(Usuario usuario, CancellationToken cancellationToken)
    {
        context.Usuarios.Add(usuario);

        try
        {
            await context.SaveChangesAsync(cancellationToken);
            return usuario;
        }
        catch (DbUpdateException exception) when (
            exception.InnerException is PostgresException postgresException &&
            postgresException.SqlState == PostgresErrorCodes.UniqueViolation &&
            postgresException.ConstraintName == "IX_usuarios_Email")
        {
            throw new EmailUsuarioJaCadastradoException();
        }
    }

    public Task<Usuario?> ObterPorIdAsync(Guid id, CancellationToken cancellationToken) =>
        context.Usuarios.AsNoTracking().SingleOrDefaultAsync(usuario => usuario.Id == id, cancellationToken);

    public async Task<IReadOnlyList<Usuario>> ListarAsync(CancellationToken cancellationToken) =>
        await context.Usuarios.AsNoTracking().OrderBy(usuario => usuario.Nome).ToListAsync(cancellationToken);
}

public sealed class EmailUsuarioJaCadastradoException : Exception;