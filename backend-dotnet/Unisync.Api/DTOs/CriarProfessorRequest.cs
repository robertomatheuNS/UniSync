using System.ComponentModel.DataAnnotations;

namespace Unisync.Api.DTOs;

public class CriarProfessorRequest : CriarUsuarioRequest
{
    [Required(ErrorMessage = "O código de professor é obrigatório.")]
    public string CodigoProfessor { get; set; } = string.Empty;
}