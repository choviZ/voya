package com.zcw.voya.langgraph4j.node;

import com.zcw.voya.langgraph4j.ai.ImageCollectionPlanService;
import com.zcw.voya.langgraph4j.model.ImageCollectionPlan;
import com.zcw.voya.langgraph4j.model.ImageResource;
import com.zcw.voya.langgraph4j.state.WorkflowContext;
import com.zcw.voya.langgraph4j.tools.ImageSearchTool;
import com.zcw.voya.langgraph4j.tools.LogoGeneratorTool;
import com.zcw.voya.langgraph4j.tools.MermaidDiagramTool;
import com.zcw.voya.langgraph4j.tools.PixabayIllustrationSearchTool;
import com.zcw.voya.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 图片收集节点
 * 使用AI进行工具调用，收集不同类型的图片
 */
@Slf4j
public class ImageCollectorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            // 获取工作流上下文
            WorkflowContext context = WorkflowContext.getContext(state);
            String originalPrompt = context.getOriginalPrompt();
            // 收集到的图片列表
            List<ImageResource> collectedImages = new ArrayList<>();
            try {
                // 1. 创建图片收集计划
                ImageCollectionPlanService planService = SpringContextUtil.getBean(ImageCollectionPlanService.class);
                ImageCollectionPlan plan = planService.planImageCollection(originalPrompt);
                log.info("获取到图片收集计划，开始并发执行");

                // 2. 并发执行图片收集任务
                // 2.1 内容图片搜索
                List<CompletableFuture<List<ImageResource>>> futures = new ArrayList<>();
                if (plan.getContentImageTasks() != null) {
                    ImageSearchTool imageSearchTool = SpringContextUtil.getBean(ImageSearchTool.class);
                    for (ImageCollectionPlan.ImageSearchTask task : plan.getContentImageTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() ->
                                imageSearchTool.searchContentImages(task.query())));
                    }
                }
                // 2.2 插画图片搜索
                if (plan.getIllustrationTasks() != null) {
                    PixabayIllustrationSearchTool illustrationSearchTool = SpringContextUtil.getBean(PixabayIllustrationSearchTool.class);
                    for (ImageCollectionPlan.IllustrationTask task : plan.getIllustrationTasks()) {
                        futures.add(CompletableFuture.supplyAsync(()
                                -> illustrationSearchTool.pixabaySearchIllustrations(task.query())));
                    }
                }
                // 2.3 架构图生成
                if (plan.getDiagramTasks() != null) {
                    MermaidDiagramTool mermaidDiagramTool = SpringContextUtil.getBean(MermaidDiagramTool.class);
                    for (ImageCollectionPlan.DiagramTask task : plan.getDiagramTasks()) {
                        futures.add(CompletableFuture.supplyAsync(()
                                -> mermaidDiagramTool.generateMermaidDiagram(task.mermaidCode(), task.description())));
                    }
                }
                // 2.4 lgo生成
                if (plan.getLogoTasks() != null) {
                    LogoGeneratorTool logoGeneratorTool = SpringContextUtil.getBean(LogoGeneratorTool.class);
                    for (ImageCollectionPlan.LogoTask task : plan.getLogoTasks()) {
                        futures.add(CompletableFuture.supplyAsync(()
                                -> logoGeneratorTool.generateLogos(task.description())));
                    }
                }
                // 等待所有任务完成并收集结果
                CompletableFuture<Void> allTask = CompletableFuture.allOf(
                        futures.toArray(new CompletableFuture[0])
                );
                allTask.join();
                // 收集所有结果
                for (CompletableFuture<List<ImageResource>> future : futures) {
                    List<ImageResource> imageResources = future.get();
                    if (imageResources != null){
                        collectedImages.addAll(imageResources);
                    }
                }
                log.info("并发收集图片完成，共收集：{}张图片", collectedImages.size());
            } catch (ExecutionException e) {
                log.error("图片收集异常", e);
            }

            // 更新状态
            context.setCurrentStep("图片收集");
            context.setImageList(collectedImages);
            return WorkflowContext.saveContext(context);
        });
    }
}
