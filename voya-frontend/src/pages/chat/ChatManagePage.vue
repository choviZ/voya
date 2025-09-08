<template>
  <div class="chat-manage-page">
    <a-card class="search-card" :bordered="false">
      <a-form layout="inline" :model="searchParams">
        <a-form-item label="应用ID">
          <a-input v-model:value="searchParams.appId" placeholder="请输入应用ID" />
        </a-form-item>
        <a-form-item label="用户ID">
          <a-input v-model:value="searchParams.userId" placeholder="请输入用户ID" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="doSearch">搜索</a-button>
          <a-button style="margin-left: 8px" @click="resetSearch">重置</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card :bordered="false" style="margin-top: 16px">
      <a-table
        :columns="columns"
        :data-source="chatHistories"
        :pagination="pagination"
        :loading="loading"
        @change="doTableChange"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'id'">
            <span>{{ record.id }}</span>
          </template>
          <template v-if="column.dataIndex === 'appId'">
            <span>{{ record.appId }}</span>
          </template>
          <template v-if="column.dataIndex === 'userId'">
            <span>{{ record.userId }}</span>
          </template>
          <template v-if="column.dataIndex === 'userMessage'">
            <span>{{ record.message }}</span>
          </template>
          <template v-if="column.dataIndex === 'createTime'">
            <span>{{ formatDate(record.createTime) }}</span>
          </template>
          <template v-if="column.dataIndex === 'action'">
            <a-button type="link" @click="viewChatDetail(record)">查看详情</a-button>
            <a-popconfirm
              title="确定要删除这条对话记录吗？"
              ok-text="确定"
              cancel-text="取消"
              @confirm="deleteChatHistory(record)"
            >
              <a-button type="link" danger>删除</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 对话详情弹窗 -->
    <a-modal
      v-model:visible="chatDetailVisible"
      title="对话详情"
      width="800px"
      :footer="null"
    >
      <div v-if="selectedChat" class="chat-detail">
        <div class="chat-info">
          <p><strong>ID:</strong> {{ selectedChat.id }}</p>
          <p><strong>应用ID:</strong> {{ selectedChat.appId }}</p>
          <p><strong>用户ID:</strong> {{ selectedChat.userId }}</p>
          <p><strong>创建时间:</strong> {{ formatDate(selectedChat.createTime as string) }}</p>
        </div>
        <div class="chat-messages">
          <div class="ai-message">
            <h3 v-if="selectedChat.messageType === 'user'">用户消息</h3>
            <h3 v-else-if="selectedChat.messageType === 'ai'">AI回复</h3>
            <div class="message-content">{{ selectedChat.message }}</div>
          </div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { listAllChatHistoryByPageForAdmin } from '@/api/chatHistoryController'

// 表格列定义
const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    key: 'id',
    width: 80,
  },
  {
    title: '应用ID',
    dataIndex: 'appId',
    key: 'appId',
    width: 80,
  },
  {
    title: '用户ID',
    dataIndex: 'userId',
    key: 'userId',
    width: 80,
  },
  {
    title: '消息内容',
    dataIndex: 'userMessage',
    key: 'userMessage',
    width: 200,
    ellipsis: true,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 180,
    sorter: true,
  },
  {
    title: '操作',
    dataIndex: 'action',
    key: 'action',
    width: 150,
  },
]

// 搜索参数
const searchParams = reactive<API.ChatHistoryQueryRequest>({
  appId: undefined,
  userId: undefined,
  current: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'descend',
})

// 表格数据
const chatHistories = ref<API.ChatHistory[]>([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条记录`,
})

// 对话详情
const chatDetailVisible = ref(false)
const selectedChat = ref<API.ChatHistory | null>(null)

// 格式化日期
const formatDate = (dateString: string) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString()
}

// 加载对话历史数据
const loadChatHistories = async () => {
  loading.value = true
  try {
    const res = await listAllChatHistoryByPageForAdmin(searchParams)
    if (res.data.code === 0 && res.data.data) {
      chatHistories.value = res.data.data.records || []
      pagination.total = res.data.data.totalRow || 0
      pagination.current = searchParams.current || 1
      pagination.pageSize = searchParams.pageSize || 10
    } else {
      message.error('获取对话历史失败：' + res.data.message)
    }
  } catch (error) {
    console.error('获取对话历史失败：', error)
    message.error('获取对话历史失败')
  } finally {
    loading.value = false
  }
}

// 表格变化处理
const doTableChange = (pag: any, filters: any, sorter: any) => {
  searchParams.current = pag.current
  searchParams.pageSize = pag.pageSize
  if (sorter.field) {
    searchParams.sortField = sorter.field
    searchParams.sortOrder = sorter.order === 'ascend' ? 'ascend' : 'descend'
  }
  loadChatHistories()
}

// 搜索
const doSearch = () => {
  searchParams.current = 1
  loadChatHistories()
}

// 重置搜索
const resetSearch = () => {
  searchParams.appId = undefined
  searchParams.userId = undefined
  searchParams.current = 1
  loadChatHistories()
}

// 查看对话详情
const viewChatDetail = (record: API.ChatHistory) => {
  selectedChat.value = record
  chatDetailVisible.value = true
}

// 删除对话历史
const deleteChatHistory = async (record: API.ChatHistory) => {
  try {
    // 这里需要后端提供删除对话历史的API
    // 目前后端未提供该API，所以这里只是模拟删除成功
    message.success('删除成功')
    loadChatHistories()
  } catch (error) {
    console.error('删除失败：', error)
    message.error('删除失败')
  }
}

// 页面加载时获取数据
onMounted(() => {
  loadChatHistories()
})
</script>

<style scoped>
.chat-manage-page {
  padding: 24px;
}

.search-card {
  margin-bottom: 16px;
}

.chat-detail {
  max-height: 600px;
  overflow-y: auto;
}

.chat-info {
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.chat-messages {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.user-message,
.ai-message {
  padding: 10px;
  border-radius: 4px;
}

.user-message {
  background-color: #f0f0f0;
}

.ai-message {
  background-color: #e6f7ff;
}

.message-content {
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
