namespace Unisync.Api.Entities;

public class Usuario
{
    public Guid Id { get; private set; } = Guid.NewGuid();
    public string Nome { get; private set; } = string.Empty;
    public string Email { get; private set; } = string.Empty;
    public string SenhaHash { get; private set; } = string.Empty;
    public DateTime DataCriacaoUtc { get; private set; } = DateTime.UtcNow;

    private Usuario()
    {
    }

    public Usuario(string nome, string email)
    {
        Nome = ValidarTexto(nome, nameof(nome), 2, 100);
        Email = ValidarTexto(email, nameof(email), 1, 180);
    }

    public void DefinirSenhaHash(string senhaHash)
    {
        if (string.IsNullOrWhiteSpace(senhaHash))
        {
            throw new ArgumentException("O hash da senha é obrigatório.", nameof(senhaHash));
        }

        SenhaHash = senhaHash;
    }

    private static string ValidarTexto(string valor, string nomeParametro, int minimo, int maximo)
    {
        if (string.IsNullOrWhiteSpace(valor))
        {
            throw new ArgumentException($"O {nomeParametro} é obrigatório.", nomeParametro);
        }

        valor = valor.Trim();
        if (valor.Length < minimo || valor.Length > maximo)
        {
            throw new ArgumentException($"O {nomeParametro} deve ter entre {minimo} e {maximo} caracteres.", nomeParametro);
        }

        return valor;
    }
}