// use-cases/register-user/register-user.use-case.ts
import type { AuthRequestDto, AuthResponse, CreateUserDto } from '@messenger/shared-schema';
import type { UserService } from '../../modules/users/index.js';
import type { NewUser, User } from '@messenger/database';
import { ConflictError, InvalidCredentialsError, UnauthorizedError } from '../../lib/errors.js';
import type { InviteService } from '../../modules/invite/index.js';

export interface JwtTokenService {
  generate(payload: { sub: string; preferred_username: string; role: 'admin' | 'user' }): string;
}

export interface HashService {
  hash(password: string): string;
  compare(password: string, passwordHash: string): boolean;
}

export function createAuthUseCase(deps: {
  userService: UserService;
  inviteService: InviteService;
  jwtTokenService: JwtTokenService;
  hashService: HashService;
}) {
  // ==============================================
  // TODO: временная заглушка для поля blackListIds
  // ==============================================
  function addMissingFields(user: User) {
    return { blackListIds: [], ...user };
  }

  return {
    async register(dto: CreateUserDto): Promise<AuthResponse> {
      const isUserExist = await deps.userService.isUserExistByUserName(dto.userName);
      if (isUserExist) {
        throw new ConflictError(`Username: ${dto.userName} is already taken`);
      }

      // Валидация токена приглашения
      await deps.inviteService.validateAndConsumeInvitation(dto.invitationToken);

      const passwordHash = deps.hashService.hash(dto.password);
      const dbUser: NewUser = {
        userName: dto.userName,
        passwordHash: passwordHash,
        profile: {
          firstName: dto.firstName,
          lastName: dto.lastName,
        },
      };

      const newUser = await deps.userService.create(dbUser);
      const jwtToken = deps.jwtTokenService.generate({
        sub: newUser.id,
        preferred_username: newUser.userName,
        role: newUser.role,
      });

      return { user: addMissingFields(newUser), token: jwtToken };
    },

    async login(dto: AuthRequestDto): Promise<AuthResponse> {
      const isUserExist = await deps.userService.isUserExistByUserName(dto.userName);

      if (!isUserExist) {
        throw new InvalidCredentialsError('Invalid username or password');
      }

      const user = await deps.userService.getByUserName(dto.userName);
      const isPasswordValid = deps.hashService.compare(dto.password, user.passwordHash);

      if (!isPasswordValid) {
        throw new InvalidCredentialsError('Invalid username or password');
      }

      const jwtPayload = {
        sub: user.id,
        preferred_username: user.userName,
        role: user.role,
      };
      const jwtToken = deps.jwtTokenService.generate(jwtPayload);

      return { user: addMissingFields(user), token: jwtToken };
    },
  };
}
