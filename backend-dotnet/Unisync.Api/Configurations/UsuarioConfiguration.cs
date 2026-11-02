using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Unisync.Api.Entities;

namespace Unisync.Api.Configurations;

public class UsuarioConfiguration : IEntityTypeConfiguration<Usuario>
{
    public void Configure(EntityTypeBuilder<Usuario> builder)
    {
        builder.ToTable("usuarios");
        builder.HasKey(usuario => usuario.Id);

        builder.Property(usuario => usuario.Nome).IsRequired().HasMaxLength(100);
        builder.Property(usuario => usuario.Email).IsRequired().HasMaxLength(180);
        builder.Property(usuario => usuario.SenhaHash).IsRequired().HasMaxLength(500);
        builder.Property(usuario => usuario.DataCriacaoUtc).IsRequired();
        builder.HasIndex(usuario => usuario.Email)
            .IsUnique()
            .HasDatabaseName("IX_usuarios_Email");
    }
}