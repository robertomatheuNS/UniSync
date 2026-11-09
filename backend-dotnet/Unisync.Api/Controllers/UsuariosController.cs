
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Unisync.Api.DTOs;
using Unisync.Api.Services;

namespace Unisync.Api.Controllers;

[ApiController]
[Route("api/usuarios")]
public class UsuariosController(UsuarioServiceInterface usuarioService) : ControllerBase
{
    [HttpPost]
    public async Task<ActionResult<UsuarioResponse>> Criar(
        CriarUsuarioRequest request,
        CancellationToken cancellationToken) =>
        ResponderCriacao(await usuarioService.CriarAsync(request, cancellationToken));

    [HttpPost("professores")]
    public async Task<ActionResult<UsuarioResponse>> CriarProfessor(
        CriarProfessorRequest request,
        CancellationToken cancellationToken) =>
        ResponderCriacao(await usuarioService.CriarProfessorAsync(request, cancellationToken));

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

    [Authorize(Roles = "Professor")]
    [HttpGet]
    public async Task<ActionResult<IReadOnlyList<UsuarioResponse>>> Listar(
        CancellationToken cancellationToken) =>
        Ok(await usuarioService.ListarAsync(cancellationToken));

    [Authorize(Roles = "Professor")]
    [HttpGet("{id:guid}")]
    public async Task<ActionResult<UsuarioResponse>> ObterPorId(
        Guid id,
        CancellationToken cancellationToken)
    {
        var usuario = await usuarioService.ObterPorIdAsync(id, cancellationToken);
        return usuario is null ? NotFound() : Ok(usuario);
    }

    private ActionResult<UsuarioResponse> ResponderCriacao(CriarUsuarioResultado resultado) =>
        resultado.Status switch
        {
            CriarUsuarioStatus.Criado when resultado.Usuario is { } usuario =>
                CreatedAtAction(nameof(ObterPorId), new { id = usuario.Id }, usuario),
            CriarUsuarioStatus.EmailJaCadastrado =>
                Conflict(new { mensagem = "O e-mail informado já está cadastrado." }),
            CriarUsuarioStatus.CodigoProfessorIndisponivel =>
                BadRequest(new { mensagem = "O código de professor informado é inválido ou já foi usado." }),
            _ => throw new InvalidOperationException("Resultado de cadastro inesperado.")
        };
}
