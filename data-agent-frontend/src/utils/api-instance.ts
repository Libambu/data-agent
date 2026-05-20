import type { AgentCard } from '@a2a-js/sdk'
import { request } from '@/utils/request'

export const api = {
  a2acontroller: {
    agentJson() {
      return request<unknown, AgentCard>({
        url: '/.well-known/agent-card.json',
        method: 'get',
      })
    },
  },
}
