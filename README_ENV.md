# Environment Variables Configuration

This project uses a `.env` file to store sensitive configuration values. The `.env` file is not committed to version control for security reasons.

## Setup

1. **Copy the example file:**
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env` file** with your actual values:
   - Database credentials
   - AWS Cognito configuration
   - AWS credentials

3. **The application will automatically load** the `.env` file on startup.

## Environment Variables

### Server Configuration
- `SERVER_PORT` - Server port (default: 8080)

### Database Configuration
- `DB_URL` - PostgreSQL connection URL
- `DB_USER` - Database username
- `DB_PASS` - Database password

### JPA/Hibernate Configuration
- `DDL_AUTO` - Hibernate DDL auto mode (default: update)
- `SHOW_SQL` - Show SQL queries in logs (default: true)

### AWS Cognito Configuration
- `COGNITO_ENABLED` - Enable/disable Cognito (default: true)
- `COGNITO_REGION` - AWS region for Cognito
- `COGNITO_USER_POOL_ID` - Cognito User Pool ID
- `COGNITO_CLIENT_ID` - Cognito App Client ID
- `COGNITO_CLIENT_SECRET` - Cognito App Client Secret
- `COGNITO_JWK_URL` - JWK URL for token validation (auto-generated if not provided)
- `COGNITO_ISSUER` - Token issuer URL (auto-generated if not provided)
- `AWS_REGION` - AWS region

### AWS Credentials
- `AWS_ACCESS_KEY_ID` - AWS access key (optional - uses default credential chain if not provided)
- `AWS_SECRET_ACCESS_KEY` - AWS secret key (optional - uses default credential chain if not provided)

## How It Works

1. The application loads the `.env` file on startup using `dotenv-java`
2. Values from `.env` are set as system properties
3. Spring Boot reads these as environment variables
4. `application.properties` uses `${VARIABLE_NAME:default}` syntax to read values

## Security Notes

- **Never commit `.env` file** to version control
- `.env` is already in `.gitignore`
- Use `.env.example` as a template for other developers
- In production, use environment variables or a secrets manager instead of `.env` files

## Fallback Behavior

If `.env` file is not found:
- Application will use environment variables if set
- Otherwise, it will use defaults from `application.properties`
- Application will still start, but some features may not work without proper configuration

