package com.zhanliao.service.impl;

import com.zhanliao.dao.ItemDOMapper;
import com.zhanliao.dao.ItemStockDOMapper;
import com.zhanliao.dataobject.ItemDO;
import com.zhanliao.dataobject.ItemStockDO;
import com.zhanliao.erro.BusinessException;
import com.zhanliao.erro.EmBusinessError;
import com.zhanliao.service.ItemService;
import com.zhanliao.service.PromoService;
import com.zhanliao.service.model.ItemModel;
import com.zhanliao.service.model.PromoModel;
import com.zhanliao.validator.ValidationImpl;
import com.zhanliao.validator.ValidationResult;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: ZhanLiao
 * @Description:
 * @Date: 2021/4/7 12:02
 * @Version: 1.0
 */
@Service
public class ItemServiceImpl implements ItemService {
    @Autowired
    ValidationImpl validator;

    @Autowired
    ItemDOMapper itemDOMapper;

    @Autowired
    ItemStockDOMapper itemStockDOMapper;

    @Autowired
    PromoService promoService;

    @Override
    @Transactional
    public ItemModel createItem(ItemModel itemModel) throws BusinessException {
        // 校验入参
        ValidationResult result = validator.validate(itemModel);
        if(result.isHasError()){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, result.getErrorMsg());
        }

        // itemModel -》 dataObject
        ItemDO itemDO = this.convertItemFromItemModel(itemModel);

        // 写入数据库
        itemDOMapper.insertSelective(itemDO);

        itemModel.setId(itemDO.getId());
        ItemStockDO itemStockDO = this.convertItemStockFromItemModel(itemModel);

        itemStockDOMapper.insertSelective(itemStockDO);
        // 返回创建完成的对象
        return this.getItemById(itemModel.getId());
    }

    private ItemDO convertItemFromItemModel(ItemModel itemModel) {
        if(itemModel == null){
            return null;
        }
        ItemDO itemDO = new ItemDO();
        BeanUtils.copyProperties(itemModel, itemDO);
        itemDO.setPrice(itemModel.getPrice().doubleValue());
        return itemDO;
    }

    private ItemStockDO convertItemStockFromItemModel(ItemModel itemModel){
        if(itemModel == null){
            return null;
        }
        final ItemStockDO itemStockDO = new ItemStockDO();
        itemStockDO.setItemId(itemModel.getId());
        itemStockDO.setStock(itemModel.getStock());
        return itemStockDO;
    }

    @Override
    public List<ItemModel> listItem() {
        /**
         * 使用stream() API将list内的itemDO ——》 itemModel
         */
        final List<ItemDO> itemDOList = itemDOMapper.listItem();
        List<ItemModel> itemModelList = itemDOList.stream().map(itemDO -> {
            final ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());
            ItemModel itemModel = this.convertModelFromDataObject(itemDO, itemStockDO);
            return itemModel;
        }).collect(Collectors.toList());
        return itemModelList;
    }

    @Override
    public ItemModel getItemById(Integer id) {
        final ItemDO itemDO = itemDOMapper.selectByPrimaryKey(id);
        if (itemDO == null) {
            return null;
        }

        // 操作获取商品库存表itemStock
        ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());

        // dataObject -》 ItemStockModel
         ItemModel itemModel = convertModelFromDataObject(itemDO, itemStockDO);

         // 获取活动商品信息
        PromoModel promoModel = promoService.getPromoByItemId(itemModel.getId());
        if(promoModel != null && promoModel.getStatus().intValue() != 3){
            itemModel.setPromoModel(promoModel);
        }
        return itemModel;
    }

    private ItemModel convertModelFromDataObject(ItemDO itemDO, ItemStockDO itemStockDO){
        final ItemModel itemModel = new ItemModel();
        BeanUtils.copyProperties(itemDO, itemModel);
        itemModel.setPrice(new BigDecimal(itemDO.getPrice()));
        itemModel.setStock(itemStockDO.getStock());
        return itemModel;
    }

    @Override
    public boolean decreaseStock(Integer itemId, Integer amount) throws BusinessException {
        /**
         * 一条SQL语句就可以解决了
         * 避免用查询语句，返货真实的库存数量，做减法，再更新库存数量
         */
        int affectRow = itemStockDOMapper.decreaseStock(itemId, amount);
        if (affectRow > 0){
            return true;
        }else {
            return false;
        }
    }

    @Override
    @Transactional
    public void increaseSales(Integer itemId, Integer amount) throws BusinessException {
        itemDOMapper.increaseSales(itemId, amount);
    }
}
