
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.AspNetCore.Mvc;
using Microsoft.IdentityModel.Tokens;
using System.Text;
using Unisync.Api.Data;
using Unisync.Api.Entities;
using Unisync.Api.Repositories;
using Unisync.Api.Services;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers();
builder.Services.Configure<ApiBehaviorOptions>(options =>
{
    options.InvalidModelStateResponseFactory = context =>
    {
        var erros = context.ModelState
            .Where(model => model.Value?.Errors.Count > 0)
            .ToDictionary(
                model => model.Key,
                model => model.Value!.Errors
                    .Select(erro => erro.ErrorMessage)
                    .ToArray()
            );

        return new BadRequestObjectResult(new
        {
            mensagem = "Existem dados inválidos na solicitação.",
            erros
        });
    };
});
builder.Services.AddOpenApi();

var neonConnection = builder.Configuration.GetConnectionString("Neon");
var connectionString = string.IsNullOrWhiteSpace(neonConnection)
    ? builder.Configuration.GetConnectionString("DefaultConnection")
    : neonConnection;
if (string.IsNullOrWhiteSpace(connectionString))
{
    throw new InvalidOperationException(
        "A conexão com o PostgreSQL não está configurada. Defina ConnectionStrings:Neon ou ConnectionStrings:DefaultConnection.");
}

builder.Services.AddDbContext<AppDbContext>(options => options.UseNpgsql(connectionString));
builder.Services.AddScoped<UsuarioRepositoryInterface, UsuarioRepository>();
builder.Services.AddScoped<UsuarioServiceInterface, UsuarioService>();
builder.Services.AddScoped<IPasswordHasher<Usuario>, PasswordHasher<Usuario>>();

var jwtKey = Convert.FromBase64String(
    builder.Configuration["Jwt:Key"]
    ?? throw new InvalidOperationException("Chave JWT não configurada."));

var jwtIssuer = builder.Configuration["Jwt:Issuer"];
var jwtAudience = builder.Configuration["Jwt:Audience"];

builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidIssuer = jwtIssuer,

            ValidateAudience = true,
            ValidAudience = jwtAudience,

            ValidateLifetime = true,

            ValidateIssuerSigningKey = true,
            IssuerSigningKey = new SymmetricSecurityKey(jwtKey)
        };
    });

builder.Services.AddAuthorization();

var app = builder.Build();

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
}

app.UseHttpsRedirection();

app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

app.Run();
