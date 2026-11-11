FROM mcr.microsoft.com/dotnet/sdk:10.0

WORKDIR /app
ENV PATH="/tools:${PATH}"
RUN dotnet tool install --tool-path /tools dotnet-ef --version 10.0.14

COPY Unisync.Api.csproj ./
RUN dotnet restore

COPY . ./
RUN dotnet build --no-restore

CMD ["sh", "-ec", "dotnet ef database update --no-build && exec dotnet bin/Debug/net10.0/Unisync.Api.dll"]
