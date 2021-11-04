package org.aim.btree;

import org.aim.entity.User;
import org.junit.jupiter.api.Test;
import java.util.Comparator;

class BTreeTest {
    private Long userId = 1l;

    /**
     * 插入关键字测试
     *
     * @return
     */
    @Test
    public void insertTest() {
        BTree<User> bTree = new BTree<User>(new Comparator() {
            @Override
            public int compare(Object user1, Object user2) {
                return Long.compare(((User) user1).getUserId(), ((User) user2).getUserId());
            }
        });
        for(int i = 0; i < 50 ; i++) {
            bTree.add(ConstructUser());
        }
        System.out.println(bTree);
        User user = bTree.search(new User().setUserId(1L));
        System.out.println(user);
        bTree.remove(new User().setUserId(25L));
    }

    private User ConstructUser() {
        User user = new User();
        user.setUserId(userId);
        user.setUserName("user"+ userId);
        user.setUserCode(String.valueOf(user.getUserName().hashCode()));
        userId++;
        return user;
    }


}