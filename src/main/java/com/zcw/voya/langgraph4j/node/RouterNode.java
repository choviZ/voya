package com.zcw.voya.langgraph4j.node;

import com.zcw.voya.ai.CodeGenTypeRoutingService;
import com.zcw.voya.ai.CodeGenTypeRoutingServiceFactory;
import com.zcw.voya.ai.model.enums.CodeGenTypeEnum;
import com.zcw.voya.langgraph4j.state.WorkflowContext;
import com.zcw.voya.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 智能路由节点
 */
@Slf4j
public class RouterNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 智能路由");

            CodeGenTypeEnum generationType;
            try {
                // 获取AI路由服务
                CodeGenTypeRoutingServiceFactory factory = SpringContextUtil.getBean(CodeGenTypeRoutingServiceFactory.class);
                CodeGenTypeRoutingService routingService = factory.createCodeGenTypeRoutingService();
                // 根据原始提示词进行智能路由
                generationType = routingService.routeCodeGenType(context.getOriginalPrompt());
                log.info("AI智能路由完成，选择类型: {} ({})", generationType.getValue(), generationType.getText());
            } catch (Exception e) {
                log.error("AI智能路由失败，使用默认HTML类型: {}", e.getMessage());
                generationType = CodeGenTypeEnum.HTML;
            }
            // 更新状态
            context.setCurrentStep("智能路由");
            context.setGenerationType(generationType);
            return WorkflowContext.saveContext(context);
        });
    }
}
