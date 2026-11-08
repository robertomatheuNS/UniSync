using Microsoft.EntityFrameworkCore;
using Npgsql;
using Unisync.Api.Data;
using Unisync.Api.Entities;

namespace Unisync.Api.Repositories;

public class UsuarioRepository(AppDbContext context) : UsuarioRepositoryInterface
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

    public async Task<bool> AdicionarProfessorComCodigoAsync(
        Professor professor,
        string codigoProfessor,
        CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(codigoProfessor))
        {
            return false;
        }

        await using var transacao = await context.Database.BeginTransactionAsync(cancellationToken);

        var codigosConsumidos = await context.Database.ExecuteSqlInterpolatedAsync($"""
            UPDATE public."Codigo_professor"
            SET ultilizado = TRUE
            WHERE codigo::text = {codigoProfessor}
              AND ultilizado = FALSE
              AND (
                  SELECT COUNT(*)
                  FROM public."Codigo_professor"
                  WHERE codigo::text = {codigoProfessor}
              ) = 1
            """, cancellationToken);

        if (codigosConsumidos != 1)
        {
            return false;
        }

        try
        {
            await AdicionarAsync(professor, cancellationToken);
            await transacao.CommitAsync(cancellationToken);
            return true;
        }
        catch
        {
            context.Entry(professor).State = EntityState.Detached;
            throw;
        }
    }

    public Task<Usuario?> ObterPorIdAsync(Guid id, CancellationToken cancellationToken) =>
        context.Usuarios.AsNoTracking().SingleOrDefaultAsync(usuario => usuario.Id == id, cancellationToken);

    public Task<Usuario?> ObterPorEmailAsync(string email, CancellationToken cancellationToken) =>
        context.Usuarios
        .AsNoTracking()
        .SingleOrDefaultAsync(usuario => usuario.Email == email, cancellationToken);

    public async Task<IReadOnlyList<Usuario>> ListarAsync(CancellationToken cancellationToken) =>
        await context.Usuarios.AsNoTracking().OrderBy(usuario => usuario.Nome).ToListAsync(cancellationToken);
}

public sealed class EmailUsuarioJaCadastradoException : Exception;
