package com.example.act.controller;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.engine.HistoryService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/**
 * /**
 *
 * @Description: 作用
 * @Author: yys
 * @Date: 2020/4/11 9:25
 * @Version: 1.0
 */
@RestController
@RequestMapping("/demo/activity")
public class ActivityTestController {


    //操作流程和静态资源
    @Autowired
    private RepositoryService repositoryService;
    //启动流程实例,删除查询动态
    @Autowired
    private RuntimeService runtimeService;
    //操作任务 任务查询任务办理
    @Autowired
    private TaskService taskService;
    //操作历史数据
    @Autowired
    private HistoryService historyService;

    @Autowired
    private ProcessEngineConfiguration processEngineConfiguration;

//    //流程实例对象
//    @Autowired
//    private ProcessInstance processInstance;

    @Autowired
    private ProcessEngine processEngine;


    private Map<String, String> map = new HashMap<>();

    @GetMapping("/test")
    public void firstDemo() {

        Deployment deployment = repositoryService
                .createDeployment()
                .addClasspathResource("processes/demo.bpmn")
                .deploy();

        ProcessDefinition processDefinition = repositoryService
                .createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();
        //会进行一些新增操作 启动流程实例
        ProcessInstance pi = runtimeService
                .startProcessInstanceById(processDefinition
                .getId());
        String processId = pi.getId();
        System.out.println("流程创建成功，当前流程实例ID：" + processId);

        Task task = taskService.createTaskQuery()
                .processInstanceId(processId)
                .singleResult();
        System.out.println("第一次执行前，任务名称：" + task.getName());
        //会删除掉掉run表中相关数据
        taskService.complete(task.getId());
    }

    /*
     * @Description: 新增流程
     * @Author:     yiys
     * @Date 2020/4/11
     * @Version:    1.0
     */
    @GetMapping("add")
    public Map<String, String> second() {
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("processes/demo.bpmn")
                .deploy();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId()).singleResult();
        //会进行一些新增操作
        ProcessInstance pi = runtimeService.startProcessInstanceById(processDefinition.getId());
        map.put("key1", pi.getId());
        //返回流程id 应该存入数据库
        return map;
    }

    /*
     * @Description: 当前任务节点
     * @Author:     yiys
     * @Date 2020/4/11
     * @Version:    1.0
     */
    @GetMapping("info/{id}")
    public String third(@PathVariable String id) {
        Task task = taskService.createTaskQuery().processInstanceId(id).singleResult();
        return "taskId:" + task.getId() + " ;taskName " + task.getName();
    }


    /*
     * @Description: 完成一次审批
     * @Author:     yiys
     * @Date 2020/4/11
     * @Version:    1.0
     */
    @GetMapping("check/{id}")
    public boolean complete(@PathVariable String id) {
        try {
            taskService.complete(id);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @GetMapping(value = "trace/data/auto/{executionId}")
    public void readResource(@PathVariable("executionId") String executionId, HttpServletResponse response) throws Exception{
        //获取到当前流程
        ProcessInstance processInstance  =   runtimeService
                .createProcessInstanceQuery()
                .processInstanceId(executionId)
                .singleResult();
        //得到bpmnModel
        BpmnModel bpmnModel = repositoryService
                .getBpmnModel(processInstance.getProcessDefinitionId());

        Collection<FlowElement> flowElements = bpmnModel.getMainProcess().getFlowElements();




    }


    private List<String> getHighLightedFlows(ProcessDefinitionEntity processDefinition, String processInstanceId) {
        List<String> highLightedFlows = new ArrayList();
        List<HistoricActivityInstance> historicActivityInstances = historyService
                .createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc().list();

        List<String> historicActivityInstanceList = new ArrayList();
        for (HistoricActivityInstance hai : historicActivityInstances) {
            historicActivityInstanceList.add(hai.getActivityId());
        }
        List<String> highLightedActivities = runtimeService.getActiveActivityIds(processInstanceId);
        historicActivityInstanceList.addAll(highLightedActivities);

        // activities and their sequence-flows
        for (ActivityImpl activity : processDefinition.getActivities()) {
            int index = historicActivityInstanceList.indexOf(activity.getId());

            if (index >= 0 && index + 1 < historicActivityInstanceList.size()) {
                List<PvmTransition> pvmTransitionList = activity
                        .getOutgoingTransitions();
                for (PvmTransition pvmTransition : pvmTransitionList) {
                    String destinationFlowId = pvmTransition.getDestination().getId();
                    if (destinationFlowId.equals(historicActivityInstanceList.get(index + 1))) {
                        highLightedFlows.add(pvmTransition.getId());
                    }
                }
            }
        }
        return highLightedFlows;
    }

    /*
     * @Description: 部署流程
     * @Author:     yiys
     * @Date 2020/4/11
     * @Version:    1.0
     */
    @GetMapping("deploy/process")
    public void deployProcess(){
        Deployment deploy = repositoryService
                .createDeployment()
                .name("demo")
                .addClasspathResource("processes.bpmn")
                .deploy();
        System.out.println(deploy.getId());
        System.out.println(deploy.getName());
    }
    /*
     * @Description: 启动流程实例
     * @Author:     yiys
     * @Date 2020/4/11
     * @Version:    1.0
     */
    @GetMapping("start/process/{processKey}")
    public void startProcessInstance(@PathVariable String processKey){
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processKey);
        System.out.println(processInstance.getId());
        System.out.println(processInstance.getName());
        System.out.println(processInstance.getLocalizedName());
        System.out.println(processInstance.getLocalizedDescription());
    }
    /*
     * @Description: 查看当前个人任务
     * @Author:     zw
     * @Date 2020/4/11
     * @Version:    1.0
     */
    @GetMapping("find/task")
    public void findMyPersonalTask(){
        String assignee = "练习";
        //创建人物查询对象  select distinct RES.* from ACT_RU_TASK RES WHERE RES.ASSIGNEE_ = ?
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(assignee)
                .list();
        for (Task t : tasks) {
            System.out.println(t.getId());
            System.out.println(t.getName());
            System.out.println(t.getCreateTime());
            System.out.println(t.getProcessInstanceId());
            System.out.println(t.getExecutionId());
            System.out.println(t.getProcessDefinitionId());
        }
        //查询
        List<Task> tasksDemo = taskService.createTaskQuery().
                taskCandidateGroup("sales").list();

    }

    /*
     * @Description: 根据条件查询任务
     * @Author:     yys
     * @Date 2020/4/11
     * @Version:    1.0
     */
    @GetMapping("find/flow")
    public void findFlow(){
        //查询
        List<Task> tasksDemo = taskService.createTaskQuery().
                taskCandidateGroup("sales").list();
    }

    /*
     * @Description: 判断流程在哪个节点
     * @Author:     yiys
     * @Date 2020/4/11
     * @Version:    1.0
     */
    @GetMapping("find/active/{id}")
    public void isProcessActive(@PathVariable("id") String id){
        ProcessInstance processInstance =  runtimeService
                .createProcessInstanceQuery()
                .processInstanceId(id)
                .singleResult();
        if (processInstance == null) {
            System.out.println("流程已经结束");
        } else {
            //获取任务状态
            System.out.println("流程未结束,节点id: " +processInstance.getActivityId());
        }

    }


    /**
     * 查询流程状态（判断流程走到哪一个节点）
     */
//    @Test
//    public void isProcessActive() {
//        String processInstanceId = "7501";
//        ProcessInstance pi = processEngine.getRuntimeService()//表示正在执行的流程实例和执行对象
//                .createProcessInstanceQuery()//创建流程实例查询
//                .processInstanceId(processInstanceId)//使用流程实例ID查询
//                .singleResult();
//        if (pi == null) {
//            log.info("流程已经结束");
//        } else {
//            log.info("流程没有结束");
//            //获取任务状态
//            log.info("节点id：" + pi.getActivityId());
//        }
//    }



}

