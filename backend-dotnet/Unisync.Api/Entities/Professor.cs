namespace Unisync.Api.Entities;

public class Professor : Usuario
{
    private Professor()
    {
    }

    public Professor(string nome, string email) : base(nome, email)
    {
    }
}
