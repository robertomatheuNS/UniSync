using System.Security.Cryptography;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Testcontainers.PostgreSql;
using Unisync.Api.Data;
using Xunit;

namespace Unisync.Api.Tests;

public sealed class ApiFixture : IAsyncLifetime
{
    public const string OrigemFrontend = "http://localhost:5173";

    private readonly PostgreSqlContainer postgres = new PostgreSqlBuilder("postgres:16").Build();
    private WebApplicationFactory<Program>? api;

    public string Conexao => postgres.GetConnectionString();

    public HttpClient CriarCliente() => api!.CreateClient(new WebApplicationFactoryClientOptions
    {
        BaseAddress = new Uri("https://localhost"),
        AllowAutoRedirect = false
    });

    public async Task InitializeAsync()
    {
        using var timeout = new CancellationTokenSource(TimeSpan.FromMinutes(2));
        await postgres.StartAsync(timeout.Token);
        api = new ApiDeTeste(Conexao);

        await using var escopo = api.Services.CreateAsyncScope();
        var banco = escopo.ServiceProvider.GetRequiredService<AppDbContext>();
        if (banco.Database.GetConnectionString() != Conexao)
        {
            throw new InvalidOperationException("A API de teste deve usar o PostgreSQL descartável.");
        }

        await banco.Database.MigrateAsync(timeout.Token);
    }

    public async Task DisposeAsync()
    {
        try
        {
            if (api is not null)
            {
                await api.DisposeAsync();
            }
        }
        finally
        {
            await postgres.DisposeAsync();
        }
    }

    private sealed class ApiDeTeste(string conexao) : WebApplicationFactory<Program>
    {
        protected override IHost CreateHost(IHostBuilder builder)
        {
            // A API resolve a conexão e a chave JWT antes de construir o host.
            builder.ConfigureHostConfiguration(configuracao => configuracao.AddInMemoryCollection(
                new Dictionary<string, string?>
                {
                    [HostDefaults.EnvironmentKey] = "Testing",
                    ["ConnectionStrings:Neon"] = " ",
                    ["ConnectionStrings:DefaultConnection"] = conexao,
                    ["Jwt:Key"] = Convert.ToBase64String(RandomNumberGenerator.GetBytes(32)),
                    ["Jwt:Issuer"] = "UniSync.Tests",
                    ["Jwt:Audience"] = "UniSync.Tests",
                    ["Cors:AllowedOrigins:0"] = OrigemFrontend
                }));

            return base.CreateHost(builder);
        }
    }
}
