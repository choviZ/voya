package com.zcw.voya.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zcw.voya.exception.BusinessException;
import com.zcw.voya.exception.ErrorCode;
import com.zcw.voya.exception.ThrowUtils;
import com.zcw.voya.model.dto.user.UserQueryRequest;
import com.zcw.voya.model.entity.User;
import com.zcw.voya.mapper.UserMapper;
import com.zcw.voya.model.enums.UserQuotaTypeEnum;
import com.zcw.voya.model.enums.UserRoleEnum;
import com.zcw.voya.model.vo.LoginUserVO;
import com.zcw.voya.model.vo.UserVO;
import com.zcw.voya.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static com.zcw.voya.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户 服务层实现
 *
 * @author zcw
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public boolean updateUserBalance(String type, User user, Integer amount) {
        UserQuotaTypeEnum quotaTypeEnum = UserQuotaTypeEnum.getEnumByValue(type);
        ThrowUtils.throwIf(quotaTypeEnum == null, ErrorCode.PARAMS_ERROR, "参数类型错误");
        // 更新额度
        User updateUser = UpdateEntity.of(User.class);
        updateUser.setId(user.getId());
        if (quotaTypeEnum == UserQuotaTypeEnum.CREATE_APP) {
            updateUser.setCreateAppLimit(user.getCreateAppLimit() + amount);
        } else if (quotaTypeEnum == UserQuotaTypeEnum.CHAT_APP) {
            updateUser.setChatLimit(user.getChatLimit() + amount);
        }
        int update = userMapper.update(updateUser);
        return update > 0;
    }

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 校验
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 检查是否重复
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        long number = this.mapper.selectCountByQuery(queryWrapper);
        if (number > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }
        // 加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 填充默认值,插入数据
        User user = User.builder()
                .userAccount(userAccount)
                .userPassword(encryptPassword)
                .userName(generateUniqueUserName())
                .userRole(UserRoleEnum.USER.getValue())
                .editTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .build();
        this.mapper.insert(user);
        return user.getId();
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        String encryptPassword = getEncryptPassword(userPassword);
        // 查询用户是否存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "用户名不存在或密码错误");
        // 记录用户登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }


    @Override
    public String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "voya";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .eq("userRole", userRole)
                .like("userAccount", userAccount)
                .like("userName", userName)
                .like("userProfile", userProfile)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }


    /**
     * 生成唯一的默认用户名，格式为"小黑子"+6位随机数
     *
     * @return 唯一的用户名
     */
    private String generateUniqueUserName() {
        String userName;
        QueryWrapper queryWrapper;
        do {
            // 生成"小黑子"+6位随机数的用户名
            String randomNum = String.format("%06d", new Random().nextInt(1000000));
            userName = "小黑子" + randomNum;

            // 检查数据库中是否已存在该用户名
            queryWrapper = new QueryWrapper();
            queryWrapper.eq("userName", userName);
        } while (this.mapper.selectCountByQuery(queryWrapper) > 0);

        return userName;
    }
}
