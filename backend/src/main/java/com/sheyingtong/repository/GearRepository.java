package com.sheyingtong.repository;

import com.sheyingtong.entity.Gear;
import com.sheyingtong.entity.GearCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface GearRepository extends JpaRepository<Gear, Long> {

    /**
     * 覆盖父接口的 findAll：category 是 EAGER 的 ManyToOne，不 join fetch 的话
     * 36 条器材会额外产生 36 次 N+1 查询（缓存冷启动回源时尤其明显）。
     */
    @Override
    @EntityGraph(attributePaths = "category")
    List<Gear> findAll();

    /** 同上，供缓存回源时按指定顺序加载 */
    @Override
    @EntityGraph(attributePaths = "category")
    List<Gear> findAll(Sort sort);

    Page<Gear> findByCategory(GearCategory category, Pageable pageable);
    
    Page<Gear> findByBrand(String brand, Pageable pageable);
    
    @Query("SELECT g FROM Gear g WHERE " +
           "(:keyword IS NULL OR g.brand LIKE %:keyword% OR g.model LIKE %:keyword%) AND " +
           "(:category IS NULL OR g.category.id = :category) AND " +
           "(:brand IS NULL OR g.brand LIKE %:brand%) AND " +
           "(:minPrice IS NULL OR g.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR g.price <= :maxPrice)")
    Page<Gear> search(
            @Param("keyword") String keyword,
            @Param("category") Long categoryId,
            @Param("brand") String brand,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );
    
    List<Gear> findTop6ByOrderByRatingDesc();
    
    @Query("SELECT DISTINCT g.brand FROM Gear g ORDER BY g.brand")
    List<String> findDistinctBrands();
    
    @Query("SELECT g FROM Gear g WHERE g.brand LIKE %:keyword% OR g.model LIKE %:keyword% OR g.description LIKE %:keyword%")
    List<Gear> searchByKeyword(@Param("keyword") String keyword);
    
    @Query("SELECT g FROM Gear g WHERE g.price >= :minPrice AND g.price <= :maxPrice ORDER BY g.price ASC")
    List<Gear> findByPriceRange(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);
}
