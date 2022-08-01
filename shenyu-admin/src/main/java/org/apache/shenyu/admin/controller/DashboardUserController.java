/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shenyu.admin.controller;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shenyu.admin.mapper.DashboardUserMapper;
import org.apache.shenyu.admin.model.custom.UserInfo;
import org.apache.shenyu.admin.model.dto.DashboardUserDTO;
import org.apache.shenyu.admin.model.page.CommonPager;
import org.apache.shenyu.admin.model.page.PageParameter;
import org.apache.shenyu.admin.model.query.DashboardUserQuery;
import org.apache.shenyu.admin.model.result.ShenyuAdminResult;
import org.apache.shenyu.admin.model.vo.DashboardUserEditVO;
import org.apache.shenyu.admin.model.vo.DashboardUserVO;
import org.apache.shenyu.admin.service.DashboardUserService;
import org.apache.shenyu.admin.utils.Assert;
import org.apache.shenyu.admin.utils.ShenyuResultMessage;
import org.apache.shenyu.admin.validation.annotation.Existed;
import org.apache.shenyu.common.utils.ShaUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * this is dashboard user controller.
 */
@Validated
@RestController
@RequestMapping("/dashboardUser")
public class DashboardUserController {
    
    private final DashboardUserService dashboardUserService;
    
    public DashboardUserController(final DashboardUserService dashboardUserService) {
        this.dashboardUserService = dashboardUserService;
    }
    
    /**
     * query dashboard users.
     *
     * @param userName    user name
     * @param currentPage current page
     * @param pageSize    page size
     * @return {@linkplain ShenyuAdminResult}
     */
    @GetMapping("")
    @RequiresPermissions("system:manager:list")
    public ShenyuAdminResult queryDashboardUsers(final String userName,
                                                 @RequestParam @NotNull(message = "currentPage not null") final Integer currentPage,
                                                 @RequestParam @NotNull(message = "pageSize not null") final Integer pageSize) {
        CommonPager<DashboardUserVO> commonPager = dashboardUserService.listByPage(new DashboardUserQuery(userName,
                new PageParameter(currentPage, pageSize)));
        
        if (CollectionUtils.isNotEmpty(commonPager.getDataList())) {
            return ShenyuAdminResult.success(ShenyuResultMessage.QUERY_SUCCESS, commonPager);
        } else {
            return ShenyuAdminResult.error(ShenyuResultMessage.DASHBOARD_QUERY_ERROR);
        }
    }
    
    /**
     * detail dashboard user.
     *
     * @param id dashboard user id.
     * @return {@linkplain ShenyuAdminResult}
     */
    @GetMapping("/{id}")
    @RequiresPermissions("system:manager:list")
    public ShenyuAdminResult detailDashboardUser(@PathVariable("id") final String id) {
        DashboardUserEditVO dashboardUserEditVO = dashboardUserService.findById(id);
        return Optional.ofNullable(dashboardUserEditVO)
                .map(item -> ShenyuAdminResult.success(ShenyuResultMessage.DETAIL_SUCCESS, item))
                .orElseGet(() -> ShenyuAdminResult.error(ShenyuResultMessage.DASHBOARD_QUERY_ERROR));
    }
    
    /**
     * create dashboard user.
     *
     * @param dashboardUserDTO dashboard user.
     * @return {@linkplain ShenyuAdminResult}
     */
    @PostMapping("")
    @RequiresPermissions("system:manager:add")
    public ShenyuAdminResult createDashboardUser(@Valid @RequestBody final DashboardUserDTO dashboardUserDTO) {
        return Optional.ofNullable(dashboardUserDTO)
                .map(item -> {
                    Assert.notBlack(item.getPassword(), ShenyuResultMessage.PARAMETER_ERROR + ": password is not balck");
                    Assert.notNull(item.getRole(), ShenyuResultMessage.PARAMETER_ERROR + ": role is not null");
                    item.setPassword(ShaUtils.shaEncryption(item.getPassword()));
                    Integer createCount = dashboardUserService.createOrUpdate(item);
                    return ShenyuAdminResult.success(ShenyuResultMessage.CREATE_SUCCESS, createCount);
                })
                .orElseGet(() -> ShenyuAdminResult.error(ShenyuResultMessage.DASHBOARD_CREATE_USER_ERROR));
    }
    
    /**
     * update dashboard user.
     *
     * @param id               primary key.
     * @param dashboardUserDTO dashboard user.
     * @return {@linkplain ShenyuAdminResult}
     */
    @PutMapping("/{id}")
    @RequiresPermissions("system:manager:edit")
    public ShenyuAdminResult updateDashboardUser(@PathVariable("id")
                                                 @Existed(provider = DashboardUserMapper.class,
                                                         message = "user is not found") final String id,
                                                 @Valid @RequestBody final DashboardUserDTO dashboardUserDTO) {
        dashboardUserDTO.setId(id);
        if (StringUtils.isNotBlank(dashboardUserDTO.getPassword())) {
            dashboardUserDTO.setPassword(ShaUtils.shaEncryption(dashboardUserDTO.getPassword()));
        }
        Integer updateCount = dashboardUserService.createOrUpdate(dashboardUserDTO);
        return ShenyuAdminResult.success(ShenyuResultMessage.UPDATE_SUCCESS, updateCount);
    }
    
    /**
     * modify dashboard user password.
     *
     * @param id               primary key.
     * @param dashboardUserDTO dashboard user.
     * @return {@linkplain ShenyuAdminResult}
     */
    @PutMapping("/modify-password/{id}")
    @RequiresPermissions("system:manager:edit")
    public ShenyuAdminResult modifyPassword(@PathVariable("id")
                                            @Existed(provider = DashboardUserMapper.class,
                                                    message = "user is not found") final String id,
                                            @Valid @RequestBody final DashboardUserDTO dashboardUserDTO) {
        UserInfo userInfo = (UserInfo) SecurityUtils.getSubject().getPrincipal();
        if (Objects.isNull(userInfo)) {
            return ShenyuAdminResult.error(ShenyuResultMessage.DASHBOARD_USER_LOGIN_ERROR);
        }
        if (!userInfo.getUserId().equals(id) && !userInfo.getUserName().equals(dashboardUserDTO.getUserName())) {
            return ShenyuAdminResult.error(ShenyuResultMessage.DASHBOARD_MODIFY_PASSWORD_ERROR);
        }
        return updateDashboardUser(id, dashboardUserDTO);
    }
    
    /**
     * delete dashboard users.
     *
     * @param ids primary key.
     * @return {@linkplain ShenyuAdminResult}
     */
    @DeleteMapping("/batch")
    @RequiresPermissions("system:manager:delete")
    public ShenyuAdminResult deleteDashboardUser(@RequestBody @NotEmpty final List<@NotBlank String> ids) {
        Integer deleteCount = dashboardUserService.delete(new HashSet<>(ids));
        return ShenyuAdminResult.success(ShenyuResultMessage.DELETE_SUCCESS, deleteCount);
    }
}
