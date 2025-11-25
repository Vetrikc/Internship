package com.innowise.internship.dao_order;

import com.innowise.internship.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long>{

}
