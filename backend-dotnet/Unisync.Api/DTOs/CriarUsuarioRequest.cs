using System.ComponentModel.DataAnnotations;

namespace Unisync.Api.DTOs;

public class CriarUsuarioRequest
{
    [Required, StringLength(100, MinimumLength = 2)]
    public string Nome { get; set; } = string.Empty;

    [Required, EmailAddress, StringLength(180)]
    public string Email { get; set; } = string.Empty;

    [Required, StringLength(128, MinimumLength = 8)]
    public string Senha { get; set; } = string.Empty;
}