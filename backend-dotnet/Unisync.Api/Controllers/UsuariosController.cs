
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Unisync.Api.DTOs;
using Unisync.Api.Services;

namespace Unisync.Api.Controllers;

[ApiController]
[Route("api/usuarios")]
public class UsuariosController(IUsuarioService usuarioService) : ControllerBase
{
    [HttpPost]
    public async Task<ActionResult<UsuarioResponse>> Criar(
        CriarUsuarioRequest request,
        CancellationToken cancellationToken)
    {
        var resultado = await usuarioService.CriarAsync(request, cancellationToken);
        if (resultado.Status == CriarUsuarioStatus.EmailJaCadastrado)
        {
            return Conflict(new { mensagem = "O e-mail informado já está cadastrado." });
        }

        return CreatedAtAction(
            nameof(ObterPorId),
            new { id = resultado.Usuario!.Id },
            resultado.Usuario);
    }

    [HttpPost("login")]
    public async Task<IActionResult> Login(
        LoginRequest request,
        CancellationToken cancellationToken)
    {
        var token = await usuarioService.LoginAsync(request, cancellationToken);

        if (token is null)
        {
            return Unauthorized(new { mensagem = "E-mail ou senha inválidos." });
        }

        return Ok(new { token });
    }

    [Authorize]
    [HttpGet]
    public async Task<ActionResult<IReadOnlyList<UsuarioResponse>>> Listar(
        CancellationToken cancellationToken) =>
        Ok(await usuarioService.ListarAsync(cancellationToken));

    [Authorize]
    [HttpGet("{id:guid}")]
    public async Task<ActionResult<UsuarioResponse>> ObterPorId(
        Guid id,
        CancellationToken cancellationToken)
    {
        var usuario = await usuarioService.ObterPorIdAsync(id, cancellationToken);
        return usuario is null ? NotFound() : Ok(usuario);
    }
}
