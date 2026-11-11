// use-cases/register-user/register-user.use-case.ts
import type { AuthRequestDto, AuthResponse, CreateUserDto } from '@messenger/shared-schema';
import type { UserService } from '../../modules/users/index.js';
import type { NewUser, User } from '@messenger/database';
import { InvalidCredentialsError, UnauthorizedError } from '../../lib/errors.js';

export interface JwtTokenService {
  generate(payload: { sub: string; preferred_username: string; role: 'admin' | 'user' }): string;
}

export interface HashService {
  hash(password: string): string;
  compare(password: string, passwordHash: string): boolean;
}

export function createAuthUseCase(deps: {
  userService: UserService;
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
    async register(dto: CreateUserDto) {
      const passwordHash = deps.hashService.hash(dto.password);

      // ==================================================================
      // TODO: Здесь будет проверка приглашения через будущий InviteService
      // ==================================================================
      const isInvitationValid = dto.invitationToken;

      const dbUser: NewUser = {
        userName: dto.userName,
        passwordHash: passwordHash,
        profile: {
          firstName: dto.firstName,
          lastName: dto.lastName,
        },
      };
      const newUser = await deps.userService.create(dbUser);

      const jwtPayload = {
        sub: newUser.id,
        preferred_username: newUser.userName,
        role: newUser.role,
      };
      const jwtToken = deps.jwtTokenService.generate(jwtPayload);

      return { user: addMissingFields(newUser), token: jwtToken };
    },

    async login(dto: AuthRequestDto) {
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
