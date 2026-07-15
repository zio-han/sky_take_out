package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.mapper.DishFlavorMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

import static com.github.pagehelper.util.ExecutorUtil.pageQuery;

@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;


    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品,{}",dishDTO);
        dishService.saveWithFlavor(dishDTO);

        //清除缓存数据
        String key = "dish_" + dishDTO.getCategoryId();

        cleanCache(key);

        return Result.success(dishDTO);
    }

    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult>page(DishPageQueryDTO dishPageQueryDTO){
        log.info("菜品分页查询");
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    @DeleteMapping()
    @ApiOperation("删除菜品")
    public Result delete(@RequestParam List<Long> ids)
    {
        log.info("菜品批量删除:#{}",ids);


        dishService.deleteBatch(ids);

        //将所有的菜品缓存数据删除掉，所有以dish_开头的key
        cleanCache("dish_*");
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("根据ID查询菜品")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("根据id查询菜品: {}",id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    @PutMapping
    @ApiOperation("修改菜品信息接口")
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("修改菜品信息:{}",dishDTO);
        dishService.updateWithFlavor(dishDTO);

        //清除缓存数据
        cleanCache("dish_*");
        return Result.success();
    }



    @GetMapping("/list")
    @ApiOperation("根据id分类查询菜品")
    public Result<List<Dish>>list(Long categoryId){
        List<Dish> list = dishService.list(categoryId);
        return Result.success(list);
    }

    /**
     * 菜品起售停售
     */
    @PostMapping("/status/{status}")
    @ApiOperation("菜品起售停售")
    public Result <String> startOrStop(@PathVariable Integer status,Long id){
        dishService.startOrStop(status,id);

        //清除缓存数据
        cleanCache("dish_*");
        return Result.success();
    }
    
    
    //清除缓存数据的通用函数
    private void cleanCache(String pattern){
        Set keys = redisTemplate.keys(pattern); // 根据匹配模式查找缓存key
        redisTemplate.delete(keys); // 删除匹配的缓存key
    }
}
