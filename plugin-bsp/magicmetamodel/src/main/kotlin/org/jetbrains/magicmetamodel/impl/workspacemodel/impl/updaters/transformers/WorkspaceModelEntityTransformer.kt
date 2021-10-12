package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.WorkspaceModelEntity

internal interface WorkspaceModelEntityBaseTransformer<in T, out R> {

  fun transform(inputEntity: T): R
}

internal interface WorkspaceModelEntityTransformer<in T, out R : WorkspaceModelEntity> :
  WorkspaceModelEntityBaseTransformer<T, R> {

  fun transform(inputEntities: List<T>): List<R> =
    inputEntities.map(this::transform).distinct()

  override fun transform(inputEntity: T): R
}

internal interface WorkspaceModelEntityPartitionTransformer<in T, out R : WorkspaceModelEntity> :
  WorkspaceModelEntityBaseTransformer<T, List<R>> {

  fun transform(inputEntities: List<T>): List<R> =
    inputEntities.flatMap(this::transform).distinct()

  override fun transform(inputEntity: T): List<R>
}
