using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Unisync.Api.Migrations
{
    /// <inheritdoc />
    public partial class ValidarTipoUsuario : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.Sql("""
                ALTER TABLE usuarios ALTER COLUMN "Tipo" DROP DEFAULT;

                -- O cadastro legado não distinguia perfis. Preserve o acesso de aluno.
                UPDATE usuarios SET "Tipo" = 1 WHERE "Tipo" = 0;
                """);

            migrationBuilder.AddCheckConstraint(
                name: "CK_usuarios_Tipo",
                table: "usuarios",
                sql: "\"Tipo\" IN (1, 2)");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropCheckConstraint(
                name: "CK_usuarios_Tipo",
                table: "usuarios");

            migrationBuilder.Sql("""
                ALTER TABLE usuarios ALTER COLUMN "Tipo" SET DEFAULT 0;
                """);
        }
    }
}
