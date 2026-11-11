using System.ComponentModel;
using System.Diagnostics;
using Microsoft.Extensions.Configuration;

namespace Unisync.Api.Comandos;

internal static class SubirNeon
{
    public const string Nome = "subir-neon";

    public static async Task<int> ExecutarAsync(string[] args)
    {
        if (args.Length != 2)
        {
            Console.Error.WriteLine($"Uso: {Nome} <caminho do secrets.json>");
            return 2;
        }

        IConfiguration configuracao;
        try
        {
            configuracao = new ConfigurationBuilder()
                .AddJsonFile(Path.GetFullPath(args[1]), optional: false)
                .Build();
        }
        catch (Exception erro) when (erro is IOException or InvalidDataException or UnauthorizedAccessException
                                     or FormatException or ArgumentException)
        {
            Console.Error.WriteLine("Não foi possível ler o secrets.json. Confira o caminho e o formato JSON.");
            return 2;
        }

        var conexao = configuracao.GetConnectionString("Neon");
        var chave = configuracao["Jwt:Key"];
        var emissor = configuracao["Jwt:Issuer"];
        var publico = configuracao["Jwt:Audience"];
        if (string.IsNullOrWhiteSpace(conexao) || string.IsNullOrWhiteSpace(chave) ||
            string.IsNullOrWhiteSpace(emissor) || string.IsNullOrWhiteSpace(publico))
        {
            Console.Error.WriteLine("O secrets.json precisa conter ConnectionStrings:Neon, Jwt:Key, Jwt:Issuer e Jwt:Audience.");
            return 2;
        }

        if (!Convert.TryFromBase64String(chave, new byte[chave.Length], out var tamanhoChave) || tamanhoChave < 32)
        {
            Console.Error.WriteLine("Jwt:Key deve estar em Base64 e representar pelo menos 32 bytes.");
            return 2;
        }

        DirectoryInfo? diretorio = new(AppContext.BaseDirectory);
        while (diretorio is not null && !File.Exists(Path.Combine(diretorio.FullName, "compose.yaml")))
        {
            diretorio = diretorio.Parent;
        }

        if (diretorio is null)
        {
            Console.Error.WriteLine("Não foi possível localizar o compose.yaml do backend.");
            return 1;
        }

        var docker = new ProcessStartInfo("docker")
        {
            WorkingDirectory = diretorio.FullName,
            UseShellExecute = false
        };
        string[] argumentos =
        [
            "compose", "-f", "compose.yaml", "-p", "unisync-neon", "run",
            "--build", "--rm", "--no-deps", "--service-ports",
            "-e", "ConnectionStrings__Neon", "-e", "Jwt__Issuer", "-e", "Jwt__Audience",
            "api", "dotnet", "bin/Debug/net10.0/Unisync.Api.dll"
        ];
        foreach (var argumento in argumentos)
        {
            docker.ArgumentList.Add(argumento);
        }

        docker.Environment["ConnectionStrings__Neon"] = conexao;
        docker.Environment["UNISYNC_JWT_KEY"] = chave;
        docker.Environment["Jwt__Issuer"] = emissor;
        docker.Environment["Jwt__Audience"] = publico;

        try
        {
            using var processo = Process.Start(docker)!;
            await processo.WaitForExitAsync();
            return processo.ExitCode;
        }
        catch (Win32Exception)
        {
            Console.Error.WriteLine("Não foi possível iniciar o Docker. Confira a instalação e se o comando docker está no PATH.");
            return 1;
        }
    }
}
