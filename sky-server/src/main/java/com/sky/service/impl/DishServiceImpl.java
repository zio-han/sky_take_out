package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.ArrayList;

import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private SetmealMapper setmealMapper;
    /**
     *
     * @param dishDTO
     */
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        //向菜品表插入1条数据
        Dish dish = new Dish();

        BeanUtils.copyProperties(dishDTO,dish);

        dishMapper.insert(dish);

        //获取insert语句的主键值
        Long dishId =  dish.getId();
        //向口味表插入n条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();

        //并不为空，且数组大于0
        if(flavors!=null && flavors.size()>0){
            flavors.forEach(dishFlavor -> {dishFlavor.setDishId(dishId);});
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }

    @Override
    public void deleteBatch(List<Long> ids) {
        //判断当前菜品是否存在起售中的菜品
        for(Long id:ids){
            Dish dish = dishMapper.getById(id);
            if(dish.getStatus() == StatusConstant.ENABLE) {
                //当前菜品处于起售中，不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //判断当前菜品是否能被删除：被套餐关联了

        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if(setmealIds != null && setmealIds.size()>0){
            //当前菜品被关联了，不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        //删除菜品表中的菜品数据
//        for(Long id:ids){
//            dishMapper.deleteById(id);
//            //删除菜品关联的口味数据
//            dishFlavorMapper.deleteByDishId(id);
//        }

        //根据菜品id集合批量删除菜品数据
        dishMapper.deleteByIds(ids);
        //根据菜品id集合批量删除菜品口味数据
        dishFlavorMapper.deleteByDishIds(ids);
    }

    //删除菜品表中的菜品数据
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        //根据id查询菜品数据

        Dish dish = dishMapper.getById(id);
        //根据菜品id查询口味数据

        List<DishFlavor>dishFlavors =  dishFlavorMapper.getByDishId(id);
        //将查询到的数据封装到VO中
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    /**
     * 更新菜品信息
     * @param dishDTO
     */
    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        //修改菜品表基本信息
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.update(dish);
        //删除原有的口味数据
        dishFlavorMapper.deleteByDishId(dishDTO.getId());

        //重新插入口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();

        if(flavors!=null && flavors.size()>0){
            flavors.forEach(dishFlavor -> {dishFlavor.setDishId(dishDTO.getId());});
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    @Override
    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    public List<Dish> list(Long categoryId) {
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return dishMapper.list(dish);
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    @Override
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for(Dish d:dishList){
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor>flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }
        return dishVOList;
    }

    /**
     * 菜品起售/停售
     * @param status 状态：1起售 0停售
     * @param id 菜品id
     */

    /**
     * 菜品起售/停售
     * @param status 状态：1起售 0停售
     * @param id 菜品id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        // 1. 如果是停售操作，校验是否被【在售套餐】关联
        if (status == StatusConstant.DISABLE) {
            // JDK8兼容写法，替代List.of(id)
            List<Long> singleDishId = Collections.singletonList(id);
            // 查询当前菜品关联的所有套餐ID
            List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(singleDishId);

            if (setmealIds != null && setmealIds.size() > 0) {
                // 批量查询这些套餐里 状态为起售(ENABLE)的套餐
                List<Long> enableSetmealList = setmealMapper.getEnableSetmealByIds(setmealIds);
                if (enableSetmealList != null && enableSetmealList.size() > 0) {
                    // 存在正在售卖的套餐绑定该菜品，禁止下架
                    throw new SetmealEnableFailedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
                }
            }
        }

        // 2. 更新菜品状态
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(dish);
    }
}

