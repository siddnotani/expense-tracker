const BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? ''

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export type QueryParams = Record<string, string | number | undefined>

function buildUrl(path: string, params?: QueryParams): string {
  const search = new URLSearchParams()
  if (params) {
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== '') {
        search.set(key, String(value))
      }
    }
  }
  const query = search.toString()
  return `${BASE_URL}${path}${query ? `?${query}` : ''}`
}

export async function apiGet<T>(path: string, params?: QueryParams): Promise<T> {
  const response = await fetch(buildUrl(path, params), {
    headers: { Accept: 'application/json' },
  })
  if (!response.ok) {
    throw new ApiError(response.status, `Request failed with status ${response.status}`)
  }
  return (await response.json()) as T
}
