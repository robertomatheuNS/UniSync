using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Unisync.Api.Entities;
using Unisync.Api.Entities.Enums;

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

        builder.HasDiscriminator<TipoUsuario>("Tipo")
            .HasValue<Aluno>(TipoUsuario.Aluno)
            .HasValue<Professor>(TipoUsuario.Professor);

        builder.Property<TipoUsuario>("Tipo").HasConversion<int>();

        builder.HasIndex(usuario => usuario.Email)
            .IsUnique()
            .HasDatabaseName("IX_usuarios_Email");
    }
}
