
using System.ComponentModel.DataAnnotations;

namespace Unisync.Api.DTOs;

public class CriarUsuarioRequest
{
    [Required(ErrorMessage = "O nome é obrigatório.")]
    [StringLength(100, MinimumLength = 2, ErrorMessage = "O nome deve ter entre 2 e 100 caracteres.")]
    public string Nome { get; set; } = string.Empty;

    [Required(ErrorMessage = "O e-mail é obrigatório.")]
    [EmailAddress(ErrorMessage = "Informe um endereço de e-mail válido.")]
    [StringLength(180, ErrorMessage = "O e-mail deve ter no máximo 180 caracteres.")]
    public string Email { get; set; } = string.Empty;

    [Required(ErrorMessage = "A senha é obrigatória.")]
    [StringLength(128, MinimumLength = 8, ErrorMessage = "A senha deve ter entre 8 e 128 caracteres.")]
    public string Senha { get; set; } = string.Empty;
}
