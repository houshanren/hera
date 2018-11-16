package com.dfire.graph;

import com.dfire.common.entity.HeraGroup;
import com.dfire.common.entity.model.HeraGroupBean;
import com.dfire.common.entity.model.HeraJobBean;
import com.dfire.common.entity.vo.HeraActionVo;
import com.dfire.common.enums.JobScheduleTypeEnum;
import com.dfire.common.kv.Tuple;
import com.dfire.common.service.HeraGroupService;
import com.dfire.common.service.HeraJobActionService;
import com.dfire.common.util.BeanConvertUtils;
import com.dfire.common.vo.JobStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;


/**
 * @author: <a href="mailto:lingxiao@2dfire.com">凌霄</a>
 * @time: Created in 下午5:16 2018/6/20
 * @desc 构建任务，组内存图
 */
@Component
public class JobGroupGraphTool {


    @Autowired
    private HeraGroupService heraGroupService;
    @Autowired
    private HeraJobActionService heraJobActionService;

    /**
     * 获取任务以及所在的上级组
     *
     * @param actionId
     * @return
     */
    public HeraJobBean getUpstreamJobBean(String actionId) {
        Tuple<HeraActionVo, JobStatus> tuple = heraJobActionService.findHeraActionVo(actionId);
        if (tuple != null) {
            HeraJobBean jobBean = HeraJobBean.builder()
                    .heraActionVo(tuple.getSource())
                    .jobStatus(tuple.getTarget())
                    .build();
            jobBean.setGroupBean(getUpstreamGroupBean(tuple.getSource().getGroupId()));
            return jobBean;
        }
        return null;
    }

    /**
     * 获取组所在的组以及上级组
     *
     * @param groupId
     * @return
     */
    public HeraGroupBean getUpstreamGroupBean(Integer groupId) {
        HeraGroup heraGroup = heraGroupService.findById(groupId);
        HeraGroupBean result = HeraGroupBean.builder()
                .groupVo(BeanConvertUtils.convert(heraGroup))
                .build();
        if (heraGroup != null && heraGroup.getParent() != null) {
            HeraGroupBean parentGroupBean = getUpstreamGroupBean(heraGroup.getParent());
            result.setParentGroupBean(parentGroupBean);

        }
        return result;
    }

    /**
     * 构建全局的组，job图,全局刷新页面属性结构的时候使用
     *
     * @return
     */
    public HeraGroupBean buildGlobalJobGroupBean() {
        HeraGroup rootGroup = heraGroupService.getRootGroup();
        HeraGroupBean rootGroupBean = getUpstreamGroupBean(rootGroup.getId());
        Map<String, HeraJobBean> allJobBeanMap = rootGroupBean.getAllSubJobBeans();
        for (HeraJobBean jobBean : allJobBeanMap.values()) {
            if (jobBean.getHeraActionVo().getScheduleType() == JobScheduleTypeEnum.Dependent) {
                for (String dependJobId : jobBean.getHeraActionVo().getDependencies()) {
                    HeraJobBean dependJobBean = allJobBeanMap.get(dependJobId);
                    jobBean.addUpStream(dependJobBean);
                    dependJobBean.addDownStream(jobBean);

                }
            }
        }
        return rootGroupBean;
    }

    /**
     * 构建全局的组关系图
     *
     * @return
     */
    public HeraGroupBean buildGlobalGroupBean() {
        HeraGroup rootGroup = heraGroupService.getRootGroup();
        return getUpstreamGroupBean(rootGroup.getId());
    }

}
