using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Unisync.Api.Migrations
{
    /// <inheritdoc />
    public partial class CriarTabelaCodigoProfessor : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            // Alguns bancos locais já têm uma tabela de convites criada manualmente.
            migrationBuilder.Sql("""
                CREATE TABLE IF NOT EXISTS public."Codigo_professor" (
                    codigo uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                    ultilizado boolean NOT NULL DEFAULT false
                );

                SELECT codigo::text, NOT ultilizado
                FROM public."Codigo_professor"
                LIMIT 0;
                """);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            throw new NotSupportedException(
                "A tabela de convites pode conter códigos provisionados antes da migration. Preserve os dados antes de removê-la.");
        }
    }
}
