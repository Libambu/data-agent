<script setup lang="ts">
import { Client, ClientFactory } from '@a2a-js/sdk/client'
import { type AgentCard } from '@a2a-js/sdk'
import { onMounted, reactive, ref, shallowRef } from 'vue'
import ToyHelloNode from '@/components/toy-hello-node.vue'
import { request } from '@/utils/request'

// 单个步骤的展示数据结构
interface ToyStep {
  id: string
  name: string
  content: string
  status: 'pending' | 'success'
}

// A2A SDK 提供的工厂，用 AgentCard 构造一个 Client
const factory = new ClientFactory()
// shallowRef：client 内部对象很复杂，没必要深响应
const client = shallowRef<Client | undefined>()
// 当前正在展示的步骤
const currentStep = reactive<ToyStep>({ content: '', id: '', name: '', status: 'pending' })
// 输入框双向绑定
const userInput = ref('')

// 用户回车后触发：调用 SDK 走流式发消息
const handleSend = async () => {
  if (!client.value) return
  const stream = client.value.sendMessageStream({
    message: {
      messageId: crypto.randomUUID(),
      role: 'user',
      kind: 'message',
      parts: [{ kind: 'text', text: userInput.value }],
    },
  })
  // 后端用 SSE 推送，每条事件就是一次循环
  for await (const event of stream) {
    console.log(event)
    if (event.kind === 'artifact-update') {
      const artifact = event.artifact
      if (artifact.name !== 'TOY_HELLO_NODE') continue
      const textPart = artifact.parts.find((p) => p.kind === 'text')
      if (textPart) {
        currentStep.name = artifact.name
        currentStep.content += textPart.text
        currentStep.status = 'pending'
      }
    }
    if (event.kind === 'status-update' && event.status.state === 'completed') {
      currentStep.status = 'success'
    }
  }
}

onMounted(async () => {
  // 1) 拉 AgentCard
  //    实际请求路径：GET /api/.well-known/agent-card.json
  //    经过 vite proxy 转发到：http://localhost:9933/.well-known/agent-card.json
  const agentCard = (await request.get<unknown, AgentCard>(
    '/.well-known/agent-card.json',
  )) as AgentCard
  // 2) 用 AgentCard 构造 A2A Client（SDK 内部会读 card.url 拿到 /a2a/jsonrpc 这个端点）
  client.value = await factory.createFromAgentCard(agentCard)
})
</script>

<template>
  <div>
    <toy-hello-node
      v-if="currentStep.name === 'TOY_HELLO_NODE'"
      :content="currentStep.content"
      :status="currentStep.status"
    />
    <el-input placeholder="请输入内容" v-model="userInput" @keydown.enter="handleSend"></el-input>
  </div>
</template>

<style scoped></style>
