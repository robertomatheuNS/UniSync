namespace Unisync.Api.DTOs;

public record UsuarioResponse(Guid Id, string Nome, string Email, DateTime DataCriacaoUtc);