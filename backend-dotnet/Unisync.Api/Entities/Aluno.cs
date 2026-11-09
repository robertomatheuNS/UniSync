namespace Unisync.Api.Entities;

public class Aluno : Usuario
{
    private Aluno()
    {
    }

    public Aluno(string nome, string email) : base(nome, email)
    {
    }
}
