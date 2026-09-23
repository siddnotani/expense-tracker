import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiGet } from './client'

describe('apiGet', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('omits undefined and empty query params', async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(
      new Response('{"ok":true}', { status: 200 }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await apiGet('/api/x', { a: '1', b: undefined, c: '' })

    expect(String(fetchMock.mock.calls[0][0])).toBe('/api/x?a=1')
  })

  it('throws ApiError on non-2xx responses', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn<typeof fetch>().mockResolvedValue(new Response('nope', { status: 404 })),
    )

    await expect(apiGet('/api/x')).rejects.toBeInstanceOf(ApiError)
  })
})
