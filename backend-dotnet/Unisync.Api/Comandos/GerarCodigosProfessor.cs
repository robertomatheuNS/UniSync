using Npgsql;

namespace Unisync.Api.Comandos;

internal static class GerarCodigosProfessor
{
    public const string Nome = "gerar-codigos-professor";
    private const int LimitePorExecucao = 1000;

    public static async Task<int> ExecutarAsync(string[] args, string? connectionString)
    {
        if (args.Length != 2 || !int.TryParse(args[1], out var quantidade) ||
            quantidade < 1 || quantidade > LimitePorExecucao)
        {
            Console.Error.WriteLine($"Uso: {Nome} <quantidade de 1 a {LimitePorExecucao}>");
            return 2;
        }

        if (string.IsNullOrWhiteSpace(connectionString))
        {
            Console.Error.WriteLine("Configure ConnectionStrings:Neon ou ConnectionStrings:DefaultConnection.");
            return 1;
        }

        var codigos = new List<string>(quantidade);

        try
        {
            await using var dataSource = NpgsqlDataSource.Create(connectionString);
            await using var command = dataSource.CreateCommand("""
                INSERT INTO public."Codigo_professor" (codigo, ultilizado)
                SELECT gen_random_uuid(), FALSE
                FROM generate_series(1, $1)
                RETURNING codigo::text
                """);
            command.Parameters.Add(new NpgsqlParameter<int> { TypedValue = quantidade });

            await using var reader = await command.ExecuteReaderAsync();
            while (await reader.ReadAsync())
            {
                codigos.Add(reader.GetString(0));
            }
        }
        catch (NpgsqlException)
        {
            Console.Error.WriteLine("Falha ao gerar os códigos. Confira a conexão e se a migration da tabela de códigos foi aplicada.");
            return 1;
        }

        foreach (var codigo in codigos)
        {
            Console.WriteLine(codigo);
        }

        return 0;
    }
}
