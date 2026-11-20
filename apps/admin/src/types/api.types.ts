export interface PageResponse<T> {
  content: T[]
  pageable: {
    pageNumber: number
    pageSize: number
    sort: {
      sorted: boolean
      unsorted: boolean
      empty: boolean
    }
    offset: number
    paged: boolean
    unpaged: boolean
  }
  totalElements: number
  totalPages: number
  last: boolean
  first: boolean
  size: number
  number: number
  sort: {
    sorted: boolean
    unsorted: boolean
    empty: boolean
  }
  numberOfElements: number
  empty: boolean
}

// Параметры запроса
export interface UserQueryParams {
  page?: number
  size?: number
  search?: string
  sort?: string
}

export interface UsersStat {
  totalUsers: number
  onlineUsers: number
}

export interface UpdateIsBannedUserRequest {
  isBlocked: boolean
}

export interface InvitationResponse {
  invitationUrl: string
}

export interface TotalInvitationsResponse {
  activeInvitationsCount: string
}
