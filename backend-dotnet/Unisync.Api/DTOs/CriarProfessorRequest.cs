using System.ComponentModel.DataAnnotations;

namespace Unisync.Api.DTOs;

public class CriarProfessorRequest : CriarUsuarioRequest
{
    [Required]
    public string CodigoProfessor { get; set; } = string.Empty;
}
