<template>
  <a-layout-header class="header">
    <a-row :wrap="false">
      <!-- 左侧：Logo和标题 -->
      <a-col flex="200px">
        <RouterLink to="/">
          <div class="header-left">
            <img class="logo" src="@/assets/logo.jpg" alt="Logo" />
            <h1 class="site-title">AI应用生成</h1>
          </div>
        </RouterLink>
      </a-col>
      <!-- 中间：导航菜单 -->
      <a-col flex="auto">
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="horizontal"
          :items="menuItems"
          @click="handleMenuClick"
        />
      </a-col>
      <!-- 右侧：用户操作区域 -->
      <a-col flex="260px">
        <div class="user-login-status">
          <div v-if="loginUserStore.loginUser.id">
            <a-dropdown>
              <a-space>
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
                {{ loginUserStore.loginUser.userName ?? '无名' }}
              </a-space>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <div class="usage-stats">
                      <div class="usage-item">
                        <div class="usage-label">应用数量</div>
                          <a-progress
                            :percent="appUsagePercent"
                            :show-info="false"
                            size="small"
                            :stroke-color="appUsagePercent >= 90 ? '#ff4d4f' : '#1890ff'"
                          />
                          <div class="usage-count">
                            {{ usedAppCount }}/{{ totalAppCount }}
                          </div>
                      </div>
                      <div class="usage-item">
                        <div class="usage-label">对话次数</div>
                          <a-progress
                            :percent="chatUsagePercent"
                            :show-info="false"
                            size="small"
                            :stroke-color="chatUsagePercent >= 90 ? '#ff4d4f' : '#1890ff'"
                          />
                          <div class="usage-count">
                            {{ usedChatCount }}/{{ totalChatCount }}
                          </div>
                      </div>
                    </div>
                  </a-menu-item>
                  <a-menu-item @click="doLogout">
                    <LogoutOutlined />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
          <div v-else>
            <a-button type="primary" href="/user/login">登录</a-button>
          </div>
        </div>
      </a-col>
    </a-row>
  </a-layout-header>
</template>

<script setup lang="ts">
import { computed, h, ref } from 'vue'
import { useRouter } from 'vue-router'
import { type MenuProps, message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { userLogout } from '@/api/userController.ts'
import { LogoutOutlined, HomeOutlined } from '@ant-design/icons-vue'

const loginUserStore = useLoginUserStore()
const router = useRouter()
// 当前选中菜单
const selectedKeys = ref<string[]>(['/'])

// 默认应用和对话次数
const DEFAULT_APP_COUNT = 10
const DEFAULT_CHAT_COUNT = 100

// 计算应用使用情况
const totalAppCount = computed(() => DEFAULT_APP_COUNT)
const remainingAppCount = computed(() => loginUserStore.loginUser.createAppLimit ?? DEFAULT_APP_COUNT)
const usedAppCount = computed(() => totalAppCount.value - remainingAppCount.value)
const appUsagePercent = computed(() => Math.round((usedAppCount.value / totalAppCount.value) * 100))

// 计算对话使用情况
const totalChatCount = computed(() => DEFAULT_CHAT_COUNT)
const remainingChatCount = computed(() => loginUserStore.loginUser.chatLimit ?? DEFAULT_CHAT_COUNT)
const usedChatCount = computed(() => totalChatCount.value - remainingChatCount.value)
const chatUsagePercent = computed(() => Math.round((usedChatCount.value / totalChatCount.value) * 100))

// 监听路由变化，更新当前选中菜单
router.afterEach((to, from, next) => {
  selectedKeys.value = [to.path]
})

// 菜单配置项
const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/admin/userManage',
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/appManage',
    label: '应用管理',
    title: '应用管理',
  },
  {
    key: '/admin/chatManage',
    label: '对话管理',
    title: '对话管理',
  },
]

// 过滤菜单项
const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    const menuKey = menu?.key as string
    if (menuKey?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

// 展示在菜单的路由数组
const menuItems = computed<MenuProps['items']>(() => filterMenus(originItems))

// 处理菜单点击
const handleMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  selectedKeys.value = [key]
  // 跳转到对应页面
  if (key.startsWith('/')) {
    router.push(key)
  }
}

// 退出登录
const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
.header {
  background: #fff;
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  height: 48px;
  width: 48px;
}

.site-title {
  margin: 0;
  font-size: 18px;
  color: #1890ff;
}

.ant-menu-horizontal {
  border-bottom: none !important;
}

.usage-stats {
  min-width: 200px;
  padding: 4px 0;
}

.usage-item {
  margin-bottom: 4px;
}

.usage-item:last-child {
  margin-bottom: 0;
}

.usage-label {
  font-size: 12px;
  margin-bottom: 2px;
  color: #666;
}

.usage-count {
  font-size: 11px;
  color: #999;
  text-align: right;
  margin-top: 1px;
}
</style>
