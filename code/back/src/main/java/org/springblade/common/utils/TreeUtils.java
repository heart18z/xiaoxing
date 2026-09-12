package org.springblade.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanUtils;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * 包: com.dfyj.bmw.common.utils
 * 作者: chukongxiang
 * 时间: 2020/3/30 - 10:22 上午
 */
public class TreeUtils {


    /**
     * 通用列表转树 基本思路 遍历子节点，找到父节点，将子节点放到父节点下，删除原子节点
     * @param list 所有节点列表
     * @param getId getId方法
     * @param getParentId getParentId方法
     * @param getChildren getChildren方法
     * @param setChildren setChildren方法
     * @param compare 默认Object::deepEquals
     * @param sort 排序方法 null 时不排序
     * @param <T>
     * @return
     */
    public static <T,R> List<R> parseTree(List<T> list, Class<R> treeVo, Function<R,Object> getId, Function<R,Object> getParentId, Function<R,List<R>> getChildren, BiConsumer<R, List<R>> setChildren, BiPredicate<Object,Object> compare, Comparator<R> sort) {
        return parseTree(list, treeVo, getId, getParentId, getChildren, setChildren, compare, sort, node->{
            R r;
            try {
                r = treeVo.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
                return null;
            }
            BeanUtils.copyProperties(node, r);
            return r;
        });
    }
    public static <T,R> List<R> parseTree(List<T> list, Class<R> treeVo, Function<R,Object> getId, Function<R,Object> getParentId, Function<R,List<R>> getChildren, BiConsumer<R, List<R>> setChildren, BiPredicate<Object,Object> compare, Comparator<R> sort, Function<T, R> map){
        List<R> rs;
        try {
            if (map==null) {
				ObjectMapper objectMapper = new ObjectMapper();
                rs = list.stream().map(t -> {
					try {
						String json = objectMapper.writeValueAsString(t);
						return objectMapper.readValue(json, treeVo);
					} catch (JsonProcessingException e) {
						throw new RuntimeException(e);
					}
                }).collect(Collectors.toList());
            } else {
                rs = list.stream().map(map).collect(Collectors.toList());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new ClassCastException("can't cast List<"+treeVo.getName()+"> to List<T>:" + list.getClass().getName() );
        }
        return parseTree(rs, getId, getParentId, getChildren, setChildren, compare, sort);
    }
    public static <T> List<T> parseTree(List<T> list,
                                        Function<T,Object> getId,
                                        Function<T,Object> getParentId,
                                        Function<T,List<T>> getChildren,
                                        BiConsumer<T, List<T>> setChildren,
                                        BiPredicate<Object,Object> compare,
                                        Comparator<T> sort) {
        List<T> copy = new ArrayList<>(list);

        //遍历顶层节点
        Iterator<T> iterator = copy.iterator();
        while (iterator.hasNext()){
            T t = iterator.next();
            Object tid = getId.apply(t);
            if (compare(tid,getParentId.apply(t),compare)) continue; //跳过id与parentId相同的项
            for (T temp : copy) { //找父节点
                if (temp == t) continue;//跳过自己
                if (compare(getId.apply(temp), getParentId.apply(temp), compare)) continue;
                boolean canRemove = setChildren(t, temp, getId, getParentId, getChildren, setChildren, compare);//递归temp节点与t节点比较
                if (canRemove) {
                    iterator.remove();
                    break;
                }
            }
        }
        //删除null节点
        List<T> tree = copy.stream().filter(Objects::nonNull).collect(Collectors.toList());
        //排序节点
        if (sort!=null) {
            tree.sort(sort);
            mapNode(tree, getChildren ,node->{
                List<T> children = getChildren.apply(node);
                if (children!=null && children.size()>0) children.sort(sort);
            });
        }
        return tree;
    }

    private static <T> boolean setChildren(T target, T father, Function<T, Object> getId, Function<T, Object> getParentId, Function<T, List<T>> getChildren, BiConsumer<T, List<T>> setChildren, BiPredicate<Object, Object> compare) {
        if(target==father)return true; //两个节点相同时直接删除target节点
        else if((target==null || father==null)) return false; //进入这里，说明两个节点不同时为null，但只要其中有一个为null则直接跳过该节点
        Object parentId = getParentId.apply(target);
        Object id = getId.apply(father);
        // 这里不能使用List list = getChildren.apply(father);list=new ArrayList<>();不能对原值操作
        // 有两种方法，一种是传入BiConsumer<T,List<T>> setChildren为father赋值，另一种是指定children属性名通过反射赋值
        // 但是反射需要写死属性名且需要处理异常，不采用
        if (getChildren.apply(father) == null) {
            if (setChildren != null) setChildren.accept(father, new ArrayList<>());
//            else {
//
//            }
        }
        List<T> apply = getChildren.apply(father);
        if (apply!=null) {
            if (compare(id, parentId, compare)) {
                apply.add(target);
                return true;
            }
            for (T t : apply) {
                boolean canRemove = setChildren(target, t, getId, getParentId, getChildren, setChildren, compare);
                if (canRemove) return canRemove;
            }
        }
        return false;
    }
    private static boolean compare(Object id, Object parentId, BiPredicate<Object,Object> compare) {
        if (parentId==id) return true;
        if (compare==null) compare = Objects::deepEquals; //
        return compare.test(id,parentId);
    }

    /**
     * 树的类型转换，一个树转另一个树
     * @param old 待转换对象
     * @param to 单节点转换方法
     * @param oldTreeGetChild 待转换对象的获取孩子节点的方法
     * @param newTreeSetChild 目标对象的设置孩子节点的方法
     * @param <R> 目标类型
     * @param <T> 待转换类型
     * @return
     */
    public static <R,T> R treeToTree(T old,
                                    Function<T, R> to,
                                    Function<T,List<T>> oldTreeGetChild,
                                    BiConsumer<R,List<R>> newTreeSetChild){

        List<T> ts = oldTreeGetChild.apply(old);
        R r = to.apply(old);
        List<R> rs = treeToTree(ts, to, oldTreeGetChild, newTreeSetChild);
        newTreeSetChild.accept(r, rs);
        return r;
    }

    /**
     * 树的类型转换 一个树转另一个树
     * @param oldTree 待转换列表
     * @param to
     * @param oldTreeGetChild
     * @param newTreeSetChild
     * @param <R>
     * @param <T>
     * @return
     */
    public static <R,T> List<R> treeToTree(List<T> oldTree,
                                           Function<T, R> to,
                                           Function<T,List<T>> oldTreeGetChild,
                                           BiConsumer<R,List<R>> newTreeSetChild){
        List<R> rs = new ArrayList<>();
        for (T t : oldTree) {
            R resolved = to.apply(t);
            List<T> oldChild = oldTreeGetChild.apply(t);
            if (oldChild != null && oldChild.size() > 0) {
                List<R> child = treeToTree(oldChild, to, oldTreeGetChild, newTreeSetChild);
                rs.add(resolved);
                newTreeSetChild.accept(resolved, child);
            } else {
                rs.add(resolved);
                newTreeSetChild.accept(resolved, new ArrayList<>());
            }
        }
        return rs;
    }


    /**
     * 根据比较方法获取 返回第一个符合条件的节点
     * @param tree
     * @param getChildren
     * @param compare target searched
     * @param <T>
     * @return
     */
    public static <T> T getChild(List<T> tree, Function<T,List<T>> getChildren, BiPredicate<T,T> compare){
        for (T t: tree){
            List<T> child = getChildren.apply(t);
            if (child!= null && child.size()>0){
                T newT = getChild(t, child, getChildren, compare);
                if (newT!=null) return newT;
            }
        }
        return null;
    }
    private static <T> T getChild(T father, List<T> tree, Function<T,List<T>> getChildren, BiPredicate<T,T> compare){
        for(T t : tree){
            boolean yes = compare.test(father, t);
            if (yes) return t;
            else {
                List<T> child = getChildren.apply(t);
                if (child!=null && child.size()>0){
                    T newT = getChild(t, child, getChildren, compare);
                    if (newT!=null) return newT;
                }
            }
        }
        return null;
    }

    /**
     * 取出某个节点的子节点
     * @param list 树
     * @param getChildren 取孩子节点的方法
     * @param compareField 比较的字段
     * @param value 比较的值
     * @param compare 值与字段的比较方法
     * @param <T>
     * @param <R>
     * @return
     */
    public static <T,R> List<T> getChildren(List<T> list, Function<T,List<T>> getChildren, Function<T, R> compareField, R value, BiPredicate<R,R> compare){
        for(T t: list){ //遍历顶级节点
            List<T> child = getChildren.apply(t);
            if (compare==null) compare = Objects::deepEquals;
            if (compare.test(compareField.apply(t),value)) return child==null?(new ArrayList<>()):child;
            if (child!=null && child.size()>0){
                //开始递归子节点查找
                List<T> cd = getChildren(child, getChildren, compareField, value, compare);
                if (cd!=null) return cd;
            }
        }
        return null;
    }

    // 取出树的所有节点 转为list
    public static <T> List<T> toArray(List<T> tree, Function<T,List<T>> getChildren){
        if (tree==null) return null;
        List<T> arr = new ArrayList<>();
        for(T t : tree){
            arr.add(t);
            List<T> child = getChildren.apply(t);
            if (child!=null && child.size()>0){
                toArray(child, getChildren, arr);
            }
        }
        return arr;
    }
    private static <T> void toArray(List<T> list, Function<T,List<T>> getChildren, List<T> all){
        for(T t : list){
            all.add(t);
            List<T> child = getChildren.apply(t);
            if (child!=null && child.size()>0){
                toArray(child, getChildren, all);
            }
        }
    }

    /**
     *
     * @param tree
     * @param getChildren
     * @param callBack t为最终节点
     * @param <T>
     */
    //遍历到最终节点时执行此函数
    public static <T> void mapFinalNode(List<T> tree, Function<T,List<T>> getChildren, Consumer<T> callBack){
        tree.forEach(t -> {
            List<T> children = getChildren.apply(t);
            if (children==null || children.size()==0) callBack.accept(t);
            else mapFinalNode(children, getChildren, callBack);
        });
    }

    public static <T> void mapFinalNode(List<T> tree, Function<T,List<T>> getChildren, int startDeep, BiConsumer<T, Integer> callBack){
        tree.forEach(t -> {
            List<T> children = getChildren.apply(t);
            if (children==null || children.size()==0) callBack.accept(t, startDeep);
            else mapFinalNode(children, getChildren, startDeep+1, callBack);
        });
    }


    //每遍历一个节点执行一次函数
    public static <T> void mapNode(List<T> tree, Function<T,List<T>> getChildren, Consumer<T> callBack){
        tree.forEach(t -> {
            callBack.accept(t);
            List<T> children = getChildren.apply(t);
            if (children!=null && children.size()>0) mapNode(children, getChildren, callBack);
        });
    }

    //每遍历一个节点执行一次函数 相比上一个传入当前深度
    public static <T> void mapNode(List<T> tree, Function<T,List<T>> getChildren, int startDeep, BiConsumer<T, Integer> callBack){
        tree.forEach(t -> {
            callBack.accept(t, startDeep);
            List<T> children = getChildren.apply(t);
            if (children!=null && children.size()>0) mapNode(children, getChildren, startDeep+1, callBack);
        });
    }
    private static <T,R> List<R> mapNode(List<T> tree, Function<T,List<T>> getChildren, Function<T, R> callBack, BiConsumer<R, List<R>> setChildren){
        List<R> newArr = new ArrayList<>();
        tree.forEach(node->{
            R newNode = callBack.apply(node);
            List<T> oldChildren = getChildren.apply(node);
            if (oldChildren!=null){
                List<R> newChildren = mapNode(oldChildren, getChildren, callBack, setChildren);
                setChildren.accept(newNode, newChildren);
            }
            newArr.add(newNode);
        });
        return newArr;
    }

    /**
     * 统计子节点数量
     * @param node
     * @param getChildren
     * @param <T>
     * @return
     */
    public static <T> int count(T node, Function<T, List<T>> getChildren) {
        List<T> children = getChildren.apply(node);
        if (children==null || children.size()==0) return 0;
        int count = children.size();
        count += children.stream().mapToInt(t -> count(t, getChildren)).sum();
        return count;
    }

    //计算最大深度
    public static <T> int maxDeep(List<T> tree, Function<T, List<T>> getChildren) {
        return maxDeep(tree, getChildren, 1);
    }
    private static <T> int maxDeep(List<T> tree, Function<T, List<T>> getChildren, int startDeep) {
        startDeep += 1;
        for (T t : tree) {
            List<T> child = getChildren.apply(t);
            if (child!=null && child.size()>0) {
                return maxDeep(child, getChildren, startDeep);
            }
        }
        return startDeep;
    }

    /**
     * 返回节点路径
     * @param list 树
     * @param getChildren 节点的getChildren方法
     * @param foundNode 该方法返回true时，返回该节点的路径
     * @param <T>
     * @return
     */
	public static <T> List<T> getPath(List<T> list, Function<T, List<T>> getChildren, Predicate<T> foundNode) {
		// 使用栈来进行深度优先搜索
		Stack<T> path = new Stack<>();
		Set<T> visited = new HashSet<>();

		for (T root : list) {
			if (findPath(root, getChildren, foundNode, path, visited)) {
				return new ArrayList<>(path);
			}
		}

		return Collections.emptyList(); // 如果没有找到路径
	}
	private static <T> boolean findPath(T node, Function<T, List<T>> getChildren, Predicate<T> foundNode,
										Stack<T> path, Set<T> visited) {
		// 如果当前节点已经访问过，直接返回
		if (visited.contains(node)) {
			return false;
		}

		// 标记节点为已访问
		visited.add(node);
		path.push(node);

		// 检查当前节点是否是目标节点
		if (foundNode.test(node)) {
			return true;
		}

		// 获取子节点并递归查找
		List<T> children = getChildren.apply(node);
		for (T child : children) {
			if (findPath(child, getChildren, foundNode, path, visited)) {
				return true;
			}
		}

		// 如果没有找到目标节点，从路径中移除当前节点
		path.pop();
		return false;
	}
}
