using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Unisync.Api.Migrations
{
    /// <inheritdoc />
    public partial class UsuariosAlunoProfessor : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "DataCriacaoUtc",
                table: "usuarios");

            migrationBuilder.AddColumn<int>(
                name: "Tipo",
                table: "usuarios",
                type: "integer",
                nullable: false,
                defaultValue: 0);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "Tipo",
                table: "usuarios");

            migrationBuilder.AddColumn<DateTime>(
                name: "DataCriacaoUtc",
                table: "usuarios",
                type: "timestamp with time zone",
                nullable: false,
                defaultValue: new DateTime(1, 1, 1, 0, 0, 0, 0, DateTimeKind.Unspecified));
        }
    }
}
