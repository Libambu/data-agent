import { type AgentCard } from '@a2a-js/sdk'
import { request } from '@/utils/request'

export const getAgentCard = async () => {
  return await request.get<AgentCard, AgentCard>('/.well-known/agent-card.json')
}
